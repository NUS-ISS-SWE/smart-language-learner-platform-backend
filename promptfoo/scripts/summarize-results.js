const fs = require('node:fs');

const CRITERIA = [
  'TASK_IDEAS',
  'ORGANISATION_COHESION',
  'GRAMMAR_SENTENCE_CONTROL',
  'VOCABULARY_WORD_CHOICE',
  'MECHANICS',
  'EXPRESSION_DESCRIPTION_VOICE',
];

const WEIGHTS = {
  TASK_IDEAS: 0.25,
  ORGANISATION_COHESION: 0.20,
  GRAMMAR_SENTENCE_CONTROL: 0.20,
  VOCABULARY_WORD_CHOICE: 0.15,
  MECHANICS: 0.10,
  EXPRESSION_DESCRIPTION_VOICE: 0.10,
};

const file = process.argv[2];
if (!file) {
  throw new Error('Usage: node scripts/summarize-results.js <promptfoo-results.json>');
}

const rows = JSON.parse(fs.readFileSync(file, 'utf8')).results.results;

function mean(values) {
  return values.reduce((sum, value) => sum + value, 0) / values.length;
}

function standardDeviation(values) {
  const average = mean(values);
  return Math.sqrt(mean(values.map((value) => (value - average) ** 2)));
}

function round(value, digits = 3) {
  return Number(value.toFixed(digits));
}

function analyseRow(row) {
  const output = typeof row.response.output === 'string'
    ? JSON.parse(row.response.output)
    : row.response.output;
  const predicted = Object.fromEntries(output.criteria.map((item) => [item.criterion, item.bandScore]));
  const expected = row.vars.expectedBands;
  const errors = CRITERIA.map((criterion) => Math.abs(predicted[criterion] - expected[criterion]));
  const predictedOverall = CRITERIA.reduce(
    (sum, criterion) => sum + predicted[criterion] * 20 * WEIGHTS[criterion],
    0,
  );
  const bias = predictedOverall - Number(row.vars.expectedOverall);

  return {
    model: row.provider.label,
    case: row.testCase.description,
    success: row.success,
    contract: row.namedScores['Contract pass'] === 1,
    grounding: row.namedScores['Evidence grounding'] === 1,
    bands: CRITERIA.map((criterion) => predicted[criterion]),
    mae: mean(errors),
    exactBands: errors.filter((error) => error === 0).length,
    maxBandError: Math.max(...errors),
    predictedOverall,
    overallError: Math.abs(bias),
    bias,
    latencyMs: row.latencyMs,
    tokens: row.response.tokenUsage.total,
    cost: row.cost || 0,
  };
}

function summarise(group) {
  return {
    runs: group.length,
    valid: group.filter((row) => row.success).length,
    contractRate: round(mean(group.map((row) => Number(row.contract)))),
    groundingRate: round(mean(group.map((row) => Number(row.grounding)))),
    meanBandMae: round(mean(group.map((row) => row.mae))),
    bandMaeSd: round(standardDeviation(group.map((row) => row.mae))),
    exactBandRate: round(group.reduce((sum, row) => sum + row.exactBands, 0) / (group.length * CRITERIA.length)),
    worstBandError: Math.max(...group.map((row) => row.maxBandError)),
    meanOverallError: round(mean(group.map((row) => row.overallError))),
    meanScoreBias: round(mean(group.map((row) => row.bias))),
    overallScoreSd: round(standardDeviation(group.map((row) => row.predictedOverall))),
    averageLatencyMs: round(mean(group.map((row) => row.latencyMs)), 0),
    totalTokens: group.reduce((sum, row) => sum + row.tokens, 0),
    estimatedCost: round(group.reduce((sum, row) => sum + row.cost, 0), 8),
  };
}

const analysed = rows.map(analyseRow);
const models = [...new Set(analysed.map((row) => row.model))];
const cases = [...new Set(analysed.map((row) => row.case))];
const summary = {
  totalRuns: analysed.length,
  models: Object.fromEntries(models.map((model) => [
    model,
    summarise(analysed.filter((row) => row.model === model)),
  ])),
  cases: Object.fromEntries(cases.map((caseName) => [
    caseName,
    Object.fromEntries(models.map((model) => {
      const group = analysed.filter((row) => row.model === model && row.case === caseName);
      return [model, {
        ...summarise(group),
        bands: group.map((row) => row.bands.join('/')),
        predictedOverall: group.map((row) => row.predictedOverall),
      }];
    })),
  ])),
};

process.stdout.write(`${JSON.stringify(summary, null, 2)}\n`);
