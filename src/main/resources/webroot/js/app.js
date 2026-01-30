// API Base URL
const API_BASE = '/api';

// Global state
let currentMasterKey = null;

// DOM Elements
const generateBtn = document.getElementById('generateBtn');
const deriveBtn = document.getElementById('deriveBtn');
const deriveMultipleBtn = document.getElementById('deriveMultipleBtn');
const encryptBtn = document.getElementById('encryptBtn');
const decryptBtn = document.getElementById('decryptBtn');

// Tab functionality
document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const tabName = btn.dataset.tab;

        // Update tab buttons
        document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');

        // Update tab contents
        document.querySelectorAll('.tab-content').forEach(content => {
            content.classList.remove('active');
        });
        document.getElementById(tabName).classList.add('active');
    });
});

// Copy to clipboard functionality
document.addEventListener('click', (e) => {
    if (e.target.classList.contains('btn-copy')) {
        const targetId = e.target.dataset.target;
        const text = document.getElementById(targetId).textContent;

        navigator.clipboard.writeText(text).then(() => {
            showToast('Copied to clipboard!', 'success');
        }).catch(() => {
            showToast('Failed to copy', 'error');
        });
    }
});

// BIP32 - Generate Master Key
generateBtn.addEventListener('click', async () => {
    const wordCount = parseInt(document.getElementById('wordCount').value);
    const customMnemonic = document.getElementById('customMnemonic').value.trim();
    const passphrase = document.getElementById('bip39Passphrase').value;

    setLoading(generateBtn, true);

    try {
        const response = await fetch(`${API_BASE}/bip32/generate`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                mnemonic: customMnemonic || null,
                wordCount: wordCount,
                passphrase: passphrase || ""
            })
        });

        const data = await handleResponse(response);

        // Store master key
        currentMasterKey = data.masterPrivateKey;

        // Display results
        document.getElementById('mnemonicOutput').textContent = data.mnemonic;
        document.getElementById('seedOutput').textContent = data.seed;
        document.getElementById('masterPrivOutput').textContent = data.masterPrivateKey;
        document.getElementById('masterPubOutput').textContent = data.masterPublicKey;

        // Show/hide passphrase status
        const passphraseStatus = document.getElementById('passphraseStatus');
        if (data.hasPassphrase) {
            passphraseStatus.style.display = 'block';
        } else {
            passphraseStatus.style.display = 'none';
        }

        // Show results sections
        document.getElementById('masterKeyResults').style.display = 'block';
        document.getElementById('derivationSection').style.display = 'block';

        showToast('Wallet generated successfully!', 'success');

    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        setLoading(generateBtn, false);
    }
});

// BIP32 - Derive Single Key
deriveBtn.addEventListener('click', async () => {
    if (!currentMasterKey) {
        showToast('Please generate a master key first', 'error');
        return;
    }

    const path = document.getElementById('derivationPath').value.trim();

    if (!path) {
        showToast('Please enter a derivation path', 'error');
        return;
    }

    setLoading(deriveBtn, true);

    try {
        const response = await fetch(`${API_BASE}/bip32/derive`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                masterKey: currentMasterKey,
                path: path
            })
        });

        const data = await handleResponse(response);

        // Display derived key
        displayDerivedKeys([data]);

        showToast('Key derived successfully!', 'success');

    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        setLoading(deriveBtn, false);
    }
});

// BIP32 - Derive Multiple Keys
deriveMultipleBtn.addEventListener('click', async () => {
    if (!currentMasterKey) {
        showToast('Please generate a master key first', 'error');
        return;
    }

    const basePath = document.getElementById('derivationPath').value.trim();

    // Replace last number with * for pattern
    const pathPattern = basePath.replace(/\/\d+$/, '/*');

    setLoading(deriveMultipleBtn, true);

    try {
        const response = await fetch(`${API_BASE}/bip32/derive-multiple`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                masterKey: currentMasterKey,
                pathPattern: pathPattern,
                count: 10
            })
        });

        const data = await handleResponse(response);

        // Display derived keys
        displayDerivedKeys(data.addresses);

        showToast(`${data.count} addresses derived successfully!`, 'success');

    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        setLoading(deriveMultipleBtn, false);
    }
});

// BIP38 - Encrypt
encryptBtn.addEventListener('click', async () => {
    const privateKey = document.getElementById('encryptPrivateKey').value.trim();
    const password = document.getElementById('encryptPassword').value;

    if (!privateKey) {
        showToast('Please enter a private key', 'error');
        return;
    }

    if (!password || password.length < 4) {
        showToast('Password must be at least 4 characters', 'error');
        return;
    }

    setLoading(encryptBtn, true);

    try {
        const response = await fetch(`${API_BASE}/bip38/encrypt`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                privateKey: privateKey,
                password: password
            })
        });

        const data = await handleResponse(response);

        // Display results
        document.getElementById('bip38Output').textContent = data.encryptedKey;
        document.getElementById('keyTypeOutput').textContent = data.compressed ? 'Compressed' : 'Uncompressed';

        document.getElementById('encryptResults').style.display = 'block';

        showToast('Private key encrypted successfully!', 'success');

    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        setLoading(encryptBtn, false);
    }
});

// BIP38 - Decrypt
decryptBtn.addEventListener('click', async () => {
    const encryptedKey = document.getElementById('decryptBip38Key').value.trim();
    const password = document.getElementById('decryptPassword').value;

    if (!encryptedKey) {
        showToast('Please enter a BIP38 encrypted key', 'error');
        return;
    }

    if (!password) {
        showToast('Please enter the password', 'error');
        return;
    }

    setLoading(decryptBtn, true);

    try {
        const response = await fetch(`${API_BASE}/bip38/decrypt`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                encryptedKey: encryptedKey,
                password: password
            })
        });

        const data = await handleResponse(response);

        // Display results
        document.getElementById('decryptedPrivateKey').textContent = data.privateKey;
        document.getElementById('decryptedAddress').textContent = data.address;
        document.getElementById('decryptedKeyType').textContent = data.compressed ? 'Compressed' : 'Uncompressed';

        document.getElementById('decryptResults').style.display = 'block';

        showToast('BIP38 key decrypted successfully!', 'success');

    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        setLoading(decryptBtn, false);
    }
});

// Helper function to display derived keys
function displayDerivedKeys(keys) {
    const container = document.getElementById('derivedKeysContainer');
    container.innerHTML = '';

    keys.forEach((key, index) => {
        const keyDiv = document.createElement('div');
        keyDiv.className = 'derived-key-item';

        const indexLabel = key.index !== undefined ? `Index ${key.index}` : `Key ${index + 1}`;

        keyDiv.innerHTML = `
            <h4>${indexLabel} - ${key.path}</h4>

            <div class="result-item">
                <label>Address:</label>
                <div class="output-box">
                    <div class="output-text mono">${key.address}</div>
                    <button class="btn-copy" data-copy="${key.address}">Copy</button>
                </div>
            </div>

            <div class="result-item">
                <label>Private Key (WIF):</label>
                <div class="output-box">
                    <div class="output-text mono">${key.privateKey}</div>
                    <button class="btn-copy" data-copy="${key.privateKey}">Copy</button>
                </div>
            </div>

            <div class="result-item">
                <label>Public Key:</label>
                <div class="output-box">
                    <div class="output-text mono">${key.publicKey}</div>
                    <button class="btn-copy" data-copy="${key.publicKey}">Copy</button>
                </div>
            </div>
        `;

        container.appendChild(keyDiv);
    });

    // Add copy functionality for derived keys
    container.querySelectorAll('.btn-copy').forEach(btn => {
        btn.addEventListener('click', () => {
            const text = btn.dataset.copy;
            navigator.clipboard.writeText(text).then(() => {
                showToast('Copied to clipboard!', 'success');
            }).catch(() => {
                showToast('Failed to copy', 'error');
            });
        });
    });

    document.getElementById('derivedResults').style.display = 'block';
}

// Handle API response
async function handleResponse(response) {
    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.error || 'An error occurred');
    }

    return data;
}

// Show toast notification
function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast ${type} show`;

    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}

// Set loading state for button
function setLoading(button, isLoading) {
    if (isLoading) {
        button.disabled = true;
        button.classList.add('loading');
        button.dataset.originalText = button.textContent;
        button.textContent = 'Processing...';
    } else {
        button.disabled = false;
        button.classList.remove('loading');
        button.textContent = button.dataset.originalText;
    }
}

// Initialize
console.log('BIP32/BIP38 Generator initialized');
