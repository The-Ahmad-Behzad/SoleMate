# PowerShell script to create .env file from env.example
Copy-Item env.example .env
Write-Host "✅ .env file created successfully!" -ForegroundColor Green
Write-Host "You can now run: npm run dev" -ForegroundColor Green

