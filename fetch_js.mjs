import https from 'https';

const url = 'https://www.vidking.net/assets/VideoPlayer-CfmbsjlB.js';

const options = {
  hostname: 'www.vidking.net',
  path: '/assets/VideoPlayer-CfmbsjlB.js',
  method: 'GET',
  headers: {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
  },
  rejectUnauthorized: false
};

const req = https.request(options, res => {
  let data = '';
  res.on('data', chunk => data += chunk);
  res.on('end', () => {
    // Find Vf component - contains server state, onServerClick, etc.
    const vfEnd = data.indexOf('Wg=') - 20; // Wg is the next component
    const vfStart = data.lastIndexOf('const Vf=', vfEnd);
    
    if (vfStart >= 0) {
      console.log('Vf component (the main player):');
      console.log(data.substring(vfStart, vfEnd));
    }
    
    // Find onServerClick
    const oscIdx = data.indexOf('onServerClick');
    if (oscIdx >= 0) {
      console.log('\n\nonServerClick context:');
      console.log(data.substring(Math.max(0, oscIdx - 200), oscIdx + 500));
    }
    
    // Find where default server is set (first server in list is tried)
    const initialIdx = data.indexOf('initial');
    if (initialIdx >= 0) {
      const ctx = data.substring(initialIdx, initialIdx + 500);
      if (ctx.includes('server')) console.log('\ninitial server context:', ctx);
    }
  });
});
req.on('error', e => console.error('Error:', e.message));
req.end();
