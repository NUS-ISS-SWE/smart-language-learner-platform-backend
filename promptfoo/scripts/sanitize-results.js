const fs = require('node:fs');

const file = process.argv[2];
if (!file) {
  throw new Error('Usage: node scripts/sanitize-results.js <promptfoo-results.json>');
}

const data = JSON.parse(fs.readFileSync(file, 'utf8'));

function removeHttpMetadata(value) {
  if (Array.isArray(value)) {
    value.forEach(removeHttpMetadata);
    return;
  }
  if (!value || typeof value !== 'object') {
    return;
  }

  if (value.metadata && typeof value.metadata === 'object') {
    delete value.metadata.http;
  }
  Object.values(value).forEach(removeHttpMetadata);
}

removeHttpMetadata(data);
fs.writeFileSync(file, `${JSON.stringify(data, null, 2)}\n`, 'utf8');

