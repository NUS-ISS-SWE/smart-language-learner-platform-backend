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

function parseOutput(output) {
  if (typeof output === 'string') {
    return JSON.parse(output);
  }
  return output;
}

function component(pass, score, reason, namedScores) {
  return { pass, score, reason, namedScores };
}

module.exports = (output, context) => {
  let result;
  try {
    result = parseOutput(output);
  } catch (error) {
    return {
      pass: false,
      score: 0,
      reason: `Output is not valid JSON: ${error.message}`,
      namedScores: {
        'Contract pass': 0,
        'Evidence grounding': 0,
        'Reference band proximity': 0,
        'Exact reference band match': 0,
        'Worst reference band proximity': 0,
        'Reference score proximity': 0,
        'Reference over-score control': 0,
      },
    };
  }

  const expectedBands = context.vars.expectedBands;
  const composition = String(context.vars.content);
  const assessments = Array.isArray(result?.criteria) ? result.criteria : [];
  const names = assessments.map((item) => item?.criterion);
  const contractPass = assessments.length === CRITERIA.length
    && CRITERIA.every((name) => names.filter((actual) => actual === name).length === 1)
    && assessments.every((item) => Number.isInteger(item?.bandScore)
      && item.bandScore >= 1 && item.bandScore <= 5
      && typeof item.rationale === 'string' && item.rationale.trim().length > 0
      && Array.isArray(item.evidence) && item.evidence.length > 0)
    && Number.isFinite(result?.confidence)
    && result.confidence >= 0 && result.confidence <= 1;

  const invalidEvidence = assessments.flatMap((item) => (item?.evidence || [])
    .filter((evidence) => typeof evidence?.excerpt !== 'string'
      || evidence.excerpt.trim().length === 0
      || !composition.includes(evidence.excerpt))
    .map((evidence) => `${item?.criterion}: ${JSON.stringify(evidence?.excerpt)}`));
  const groundingPass = assessments.length === CRITERIA.length && invalidEvidence.length === 0;

  const byCriterion = Object.fromEntries(
    assessments
      .filter((item) => CRITERIA.includes(item?.criterion) && Number.isInteger(item?.bandScore))
      .map((item) => [item.criterion, item.bandScore]),
  );
  const comparable = CRITERIA.every((name) => Number.isInteger(byCriterion[name])
    && Number.isInteger(expectedBands?.[name]));

  let mae = 4;
  let maxBandError = 4;
  let exactMatches = 0;
  let predictedOverall = 0;
  if (comparable) {
    const absoluteErrors = CRITERIA.map((name) => Math.abs(byCriterion[name] - expectedBands[name]));
    mae = absoluteErrors.reduce((sum, value) => sum + value, 0) / CRITERIA.length;
    maxBandError = Math.max(...absoluteErrors);
    exactMatches = absoluteErrors.filter((value) => value === 0).length;
    predictedOverall = CRITERIA.reduce(
      (sum, name) => sum + byCriterion[name] * 20 * WEIGHTS[name],
      0,
    );
  }

  const expectedOverall = Number(context.vars.expectedOverall);
  const signedOverallError = comparable ? predictedOverall - expectedOverall : 100;
  const overallError = Math.abs(signedOverallError);
  const bandAccuracy = Math.max(0, 1 - mae / 4);
  const exactMatchRate = exactMatches / CRITERIA.length;
  const worstBandAccuracy = Math.max(0, 1 - maxBandError / 4);
  const weightedScoreAccuracy = Math.max(0, 1 - overallError / 100);
  const overgradingControl = Math.max(0, 1 - Math.max(0, signedOverallError) / 100);
  // Pass/fail measures response validity only. Accuracy is deliberately kept
  // as continuous data so an arbitrary post-hoc threshold cannot decide the
  // model comparison.
  const pass = contractPass && groundingPass && comparable;

  const components = [
    component(
      contractPass,
      contractPass ? 1 : 0,
      contractPass ? 'Output covers the six criteria exactly once and satisfies field constraints.'
        : `Contract failure; returned criteria: ${JSON.stringify(names)}`,
      { 'Contract pass': contractPass ? 1 : 0 },
    ),
    component(
      groundingPass,
      groundingPass ? 1 : 0,
      groundingPass ? 'Every evidence excerpt is a verbatim substring of the composition.'
        : `Ungrounded evidence: ${invalidEvidence.join('; ')}`,
      { 'Evidence grounding': groundingPass ? 1 : 0 },
    ),
    component(
      comparable,
      (bandAccuracy + weightedScoreAccuracy) / 2,
      `Band MAE ${mae.toFixed(3)}; maximum band error ${maxBandError}; ${exactMatches}/6 exact bands; predicted overall ${predictedOverall.toFixed(2)} vs expected ${expectedOverall.toFixed(2)} (signed error ${signedOverallError.toFixed(2)}, absolute error ${overallError.toFixed(2)}).`,
      {
        'Reference band proximity': bandAccuracy,
        'Exact reference band match': exactMatchRate,
        'Worst reference band proximity': worstBandAccuracy,
        'Reference score proximity': weightedScoreAccuracy,
        'Reference over-score control': overgradingControl,
      },
    ),
  ];

  return {
    pass,
    score: components.reduce((sum, item) => sum + item.score, 0) / components.length,
    reason: pass
      ? `Valid grounded response; band MAE ${mae.toFixed(3)}, max band error ${maxBandError}, weighted-score error ${overallError.toFixed(2)}.`
      : `Invalid response: contract=${contractPass}, grounding=${groundingPass}, comparable=${comparable}. Accuracy metrics are reported separately.`,
    namedScores: {
      'Contract pass': contractPass ? 1 : 0,
      'Evidence grounding': groundingPass ? 1 : 0,
      'Reference band proximity': bandAccuracy,
      'Exact reference band match': exactMatchRate,
      'Worst reference band proximity': worstBandAccuracy,
      'Reference score proximity': weightedScoreAccuracy,
      'Reference over-score control': overgradingControl,
    },
    componentResults: components,
  };
};
