import http from 'http';
import fs from 'fs';
import path from 'path';

// --- Terminal Styling ---
const colors = {
    reset: '\x1b[0m',
    bright: '\x1b[1m',
    green: '\x1b[32m',
    red: '\x1b[31m',
    blue: '\x1b[34m',
    cyan: '\x1b[36m',
    yellow: '\x1b[33m'
};

const printHeader = (text) => console.log(`\n${colors.bright}${colors.blue}=== ${text} ===${colors.reset}`);
const printSubHeader = (text) => console.log(`\n${colors.cyan}--- ${text} ---${colors.reset}`);

// --- Request Helper ---
async function makeRequest(path, method, body, isMultipart = false) {
    return new Promise((resolve, reject) => {
        const headers = {
            'Authorization': 'Bearer test_token'
        };

        let postData = '';
        if (isMultipart) {
            const boundary = `----WebKitFormBoundary${Math.random().toString(36).substring(2)}`;
            headers['Content-Type'] = `multipart/form-data; boundary=${boundary}`;
            
            let data = '';
            for (const key in body) {
                data += `--${boundary}\r\n`;
                if (key === 'textureFile' || key === 'snapshot') {
                    data += `Content-Disposition: form-data; name="${key}"; filename="test.png"\r\n`;
                    data += 'Content-Type: image/png\r\n\r\n';
                    data += 'DUMMY_IMAGE_DATA'; // In a real test we might read a file, but for BFF logic verification this is enough
                    data += '\r\n';
                } else {
                    data += `Content-Disposition: form-data; name="${key}"\r\n\r\n`;
                    data += body[key] + '\r\n';
                }
            }
            data += `--${boundary}--\r\n`;
            postData = data;
        } else if (body) {
            headers['Content-Type'] = 'application/json';
            postData = JSON.stringify(body);
        }

        const options = {
            hostname: 'localhost',
            port: 8080,
            path,
            method,
            headers
        };

        const req = http.request(options, (res) => {
            let resBody = '';
            res.on('data', (chunk) => resBody += chunk);
            res.on('end', () => {
                let parsedBody = resBody;
                try {
                    parsedBody = JSON.parse(resBody);
                } catch (e) {}
                resolve({ statusCode: res.statusCode, body: parsedBody });
            });
        });

        req.on('error', reject);
        if (postData) req.write(postData);
        req.end();
    });
}

function assert(actual, expected, name) {
    const success = actual === expected;
    const icon = success ? `${colors.green}✅ PASS` : `${colors.red}❌ FAIL`;
    console.log(`${icon}${colors.reset} [${name}] Status: ${actual}${!success ? ` (Expected: ${expected})` : ''}`);
    return success;
}

// --- Test Suite ---
async function runTests() {
    printHeader('SoleMate BFF Comprehensive API Verification');

    const testUserUid = '000000000000000000000123';
    let sharedSkinId = null;
    let sharedShoeId = '6900f235777b45b41bafff98'; // Fallback to user's known shoe

    // 1. Health Checks
    printSubHeader('1. System Health');
    const h1 = await makeRequest('/health', 'GET');
    assert(h1.statusCode, 200, 'GET /health');

    const h2 = await makeRequest('/api/health/storage', 'GET');
    assert(h2.statusCode, 200, 'GET /api/health/storage');
    console.log(`   Storage Mode: ${h2.body.storageType}`);

    // 2. Catalog
    printSubHeader('2. Catalog Endpoints');
    const c1 = await makeRequest('/api/catalog', 'GET');
    assert(c1.statusCode, 200, 'GET /api/catalog');
    if (c1.body.length > 0) {
        sharedShoeId = c1.body[0]._id;
        console.log(`   Found Active Shoe ID: ${sharedShoeId}`);
    }

    const c2 = await makeRequest(`/api/catalog/${sharedShoeId}`, 'GET');
    assert(c2.statusCode, 200, 'GET /api/catalog/:id');

    const c3 = await makeRequest(`/api/catalog/search?q=Runner`, 'GET');
    assert(c3.statusCode, 200, 'GET /api/catalog/search');

    // 3. User Profile
    printSubHeader('3. User Profile');
    const u1 = await makeRequest('/api/user/profile', 'PUT', { name: 'Verified Test User', preferences: { shoeSize: 10 } });
    assert(u1.statusCode, 200, 'PUT /api/user/profile');

    const u2 = await makeRequest('/api/user/profile', 'GET');
    assert(u2.statusCode, 200, 'GET /api/user/profile');
    console.log(`   User Name: ${u2.body.name}`);

    // 4. Custom Skins
    printSubHeader('4. Custom Skins');
    const s1 = await makeRequest('/api/skins/create', 'POST', { 
        shoeId: sharedShoeId, 
        skinName: 'Automated Test Skin',
        textureFile: 'binary_placeholder'
    }, true);
    assert(s1.statusCode, 201, 'POST /api/skins/create (Multipart)');
    if (s1.body._id) sharedSkinId = s1.body._id;

    const s2 = await makeRequest('/api/skins', 'GET');
    assert(s2.statusCode, 200, 'GET /api/skins');

    // 5. Try-On History
    printSubHeader('5. Try-On History');
    const t1 = await makeRequest('/api/tryon/save', 'POST', {
        shoeId: sharedShoeId,
        snapshot: 'binary_placeholder'
    }, true);
    assert(t1.statusCode, 201, 'POST /api/tryon/save (Multipart)');

    const t2 = await makeRequest('/api/tryon/history', 'GET');
    assert(t2.statusCode, 200, 'GET /api/tryon/history');

    // 6. Outfit Analysis
    printSubHeader('6. Outfit Analysis & AI');
    const a1 = await makeRequest('/api/outfit/analyze', 'POST', {
        outfitImageUrl: 'https://example.com/test.jpg',
        dominantColors: ['blue', 'white']
    });
    assert(a1.statusCode, 201, 'POST /api/outfit/analyze');

    const a2 = await makeRequest('/api/outfit/recommend', 'POST', {
        colors: ['red', 'black']
    });
    // This might take longer due to AI cold start on Render
    assert(a2.statusCode, 200, 'POST /api/outfit/recommend (AI Search)');
    if (a2.body.length > 0) console.log(`   AI Result: ${a2.body[0].name}`);

    // 7. Cleanup & Stats
    printSubHeader('7. Final Stats & Cleanup');
    if (sharedSkinId) {
        const d1 = await makeRequest(`/api/skins/${sharedSkinId}`, 'DELETE');
        assert(d1.statusCode, 204, 'DELETE /api/skins/:id');
    }

    const st1 = await makeRequest('/api/user/stats', 'GET');
    assert(st1.statusCode, 200, 'GET /api/user/stats');
    console.log(`   Final Try-On Count: ${st1.body.tryOnCount}`);

    printHeader('ALL TESTS COMPLETE');
}

runTests().catch(err => {
    console.error(`\n${colors.red}CRITICAL TEST FAILURE:${colors.reset}`);
    console.error(err);
});
