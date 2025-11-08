function numeric(value, fallback = 0) {
    const num = Number(value);
    return Number.isFinite(num) ? num : fallback;
}

function scorePeers(peers, weights) {
    const alpha = weights.alphaSpeed ?? 0.6;
    const beta = weights.betaLatency ?? 0.002;
    const gamma = weights.gammaReliability ?? 0.4;
    return peers
        .map((peer) => {
            const metrics = peer.metrics || {};
            const uploadSpeed = numeric(metrics.uploadSpeed);
            const latency = numeric(metrics.latency, 999);
            const reliability = numeric(metrics.successRate, 0.5);
            const score = alpha * uploadSpeed - beta * latency + gamma * reliability;
            return {
                ...peer,
                metrics: {
                    uploadSpeed,
                    latency,
                    successRate: reliability
                },
                score
            };
        })
        .sort((a, b) => b.score - a.score);
}

module.exports = {
    scorePeers
};
