import fs from 'fs';

// Read env.example
const envExample = fs.readFileSync('./env.example', 'utf8');

// Write to .env
fs.writeFileSync('./.env', envExample);

console.log('✅ .env file created successfully!');
console.log('You can now run: npm run seed');


