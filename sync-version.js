import fs from 'fs';
import path from 'path';

// Read version from package.json
const packageJson = JSON.parse(fs.readFileSync('package.json', 'utf8'));
const version = packageJson.version;
// Strip tag for tauri conf (MSI/installer often needs x.y.z)
const shortVersion = version.split('-')[0];

console.log(`Syncing version ${version}...`);

// Update tauri.conf.json
const tauriConfPath = path.join('src-tauri', 'tauri.conf.json');
const tauriConf = JSON.parse(fs.readFileSync(tauriConfPath, 'utf8'));
tauriConf.version = shortVersion;
fs.writeFileSync(tauriConfPath, JSON.stringify(tauriConf, null, 2) + '\n');
console.log(`✓ Updated ${tauriConfPath}`);

// Update Cargo.toml
const cargoTomlPath = path.join('src-tauri', 'Cargo.toml');
let cargoToml = fs.readFileSync(cargoTomlPath, 'utf8');
cargoToml = cargoToml.replace(/^version = ".*"$/m, `version = "${version}"`);
fs.writeFileSync(cargoTomlPath, cargoToml);
console.log(`✓ Updated ${cargoTomlPath}`);

console.log('Version sync complete!');
