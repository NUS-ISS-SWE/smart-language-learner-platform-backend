// Chat Completions provider for reasoning models promptfoo 0.120.19 does not recognise (it only
// treats o1/o3/o4/gpt-5* as reasoning models). For any other name promptfoo's built-in provider
// always sends max_tokens and temperature, which reasoning models reject with a 400. This sends
// exactly what production sends: max_completion_tokens and reasoning_effort, no temperature.
//
// config: model, reasoning_effort, max_completion_tokens, schema (path relative to promptfoo-evidence/),
//         input_price_per_million / output_price_per_million in USD (for the cost column).
const fs = require('fs');
const path = require('path');

const MAX_ATTEMPTS = 4;

class OpenAiReasoningProvider {
  constructor(options = {}) {
    this.config = options.config || {};
    this.providerId = options.id || `openai-reasoning:${this.config.model}`;
    this.responseFormat = this.config.schema
      ? JSON.parse(fs.readFileSync(path.resolve(__dirname, '..', this.config.schema), 'utf8'))
      : undefined;
  }

  id() {
    return this.providerId;
  }

  async callApi(prompt) {
    const apiKey = process.env.OPENAI_API_KEY;
    if (!apiKey) return { error: 'OPENAI_API_KEY is not set' };

    let messages;
    try {
      messages = JSON.parse(prompt);
      if (!Array.isArray(messages)) messages = [{ role: 'user', content: prompt }];
    } catch {
      messages = [{ role: 'user', content: prompt }];
    }

    const body = {
      model: this.config.model,
      messages,
      ...(this.config.reasoning_effort ? { reasoning_effort: this.config.reasoning_effort } : {}),
      ...(this.config.max_completion_tokens ? { max_completion_tokens: this.config.max_completion_tokens } : {}),
      ...(this.responseFormat ? { response_format: this.responseFormat } : {}),
    };

    let lastError;
    for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      let res;
      let text;
      try {
        res = await fetch('https://api.openai.com/v1/chat/completions', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${apiKey}` },
          body: JSON.stringify(body),
        });
        text = await res.text();
      } catch (error) {
        // Network failure (e.g. "fetch failed"): retry like a server error.
        lastError = `Network error: ${error.message}`;
        await new Promise((resolve) => setTimeout(resolve, 2000 * attempt));
        continue;
      }
      if (res.ok) return this.toResult(JSON.parse(text));
      lastError = `API error: ${res.status} ${res.statusText}\n${text}`;
      // Only rate limits and server errors are worth retrying; a 400 will fail the same way again.
      if (res.status !== 429 && res.status < 500) break;
      await new Promise((resolve) => setTimeout(resolve, 2000 * attempt));
    }
    return { error: lastError };
  }

  toResult(data) {
    const choice = data.choices?.[0];
    const usage = data.usage || {};
    const promptTokens = usage.prompt_tokens || 0;
    const completionTokens = usage.completion_tokens || 0; // includes reasoning tokens
    const inPrice = this.config.input_price_per_million;
    const outPrice = this.config.output_price_per_million;
    return {
      output: choice?.message?.content ?? '',
      tokenUsage: {
        total: usage.total_tokens || promptTokens + completionTokens,
        prompt: promptTokens,
        completion: completionTokens,
        completionDetails: { reasoning: usage.completion_tokens_details?.reasoning_tokens || 0 },
      },
      ...(inPrice !== undefined && outPrice !== undefined
        ? { cost: (promptTokens * inPrice + completionTokens * outPrice) / 1e6 }
        : {}),
      ...(choice?.finish_reason === 'length' ? { metadata: { finishReason: 'length' } } : {}),
    };
  }
}

module.exports = OpenAiReasoningProvider;
