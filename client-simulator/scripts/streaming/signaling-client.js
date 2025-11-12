const WebSocket = require('ws');
const { EventEmitter } = require('events');

class SignalingClient extends EventEmitter {
    constructor({ url, clientId, streamId, logger }) {
        super();
        this.url = url;
        this.clientId = clientId;
        this.streamId = streamId;
        this.log = logger;
        this.socket = null;
        this.pendingWhoHas = new Map(); // segmentId -> {resolve, reject, timer}
        this.connected = false;
    }

    async connect() {
        if (this.connected) {
            return;
        }
        const urlWithQuery = `${this.url}?clientId=${encodeURIComponent(this.clientId)}&streamId=${encodeURIComponent(this.streamId)}`;
        await new Promise((resolve, reject) => {
            const ws = new WebSocket(urlWithQuery);
            let settled = false;

            ws.once('open', () => {
                this.socket = ws;
                this.connected = true;
                settled = true;
                this.log.info(`Connected to signaling as ${this.clientId}`);
                resolve();
            });

            ws.once('error', (err) => {
                if (!settled) {
                    settled = true;
                    reject(err);
                } else {
                    this.log.error(`Signaling socket error: ${err.message}`);
                    this.emit('error', err);
                }
            });

            ws.on('message', (data) => this.handleMessage(data));

            ws.once('close', (code, reason) => {
                this.connected = false;
                this.socket = null;
                const closeInfo = reason ? `${code} (${reason.toString()})` : `${code}`;
                this.log.warn(`Signaling connection closed: ${closeInfo}`);
                this.cleanupPending(new Error('Signaling connection closed'));
                this.emit('close', { code, reason });
            });
        });
    }

    async requestWhoHas(segmentId, timeoutMs) {
        if (!this.connected || !this.socket) {
            throw new Error('Signaling connection is not ready');
        }
        if (this.pendingWhoHas.has(segmentId)) {
            return this.pendingWhoHas.get(segmentId).promise;
        }
        const payload = {
            type: 'WHO_HAS',
            streamId: this.streamId,
            segmentId
        };
        const promise = new Promise((resolve, reject) => {
            const timer = setTimeout(() => {
                this.pendingWhoHas.delete(segmentId);
                resolve([]);
            }, timeoutMs);
            this.pendingWhoHas.set(segmentId, { resolve, reject, timer });
            try {
                this.socket.send(JSON.stringify(payload));
            } catch (error) {
                clearTimeout(timer);
                this.pendingWhoHas.delete(segmentId);
                reject(error);
            }
        });
        this.pendingWhoHas.get(segmentId).promise = promise;
        return promise;
    }

    async reportSegment({ segmentId, source, latencyMs, speedMbps }) {
        if (!this.connected || !this.socket) {
            return;
        }
        const payload = {
            type: 'REPORT_SEGMENT',
            streamId: this.streamId,
            segmentId,
            source,
            latency: Math.round(latencyMs || 0),
            speed: Number.isFinite(speedMbps) ? Number(speedMbps.toFixed(3)) : 0
        };
        try {
            this.socket.send(JSON.stringify(payload));
        } catch (error) {
            this.log.warn(`Failed to report segment ${segmentId}: ${error.message}`);
        }
    }

    handleMessage(raw) {
        let message;
        try {
            message = JSON.parse(raw.toString());
        } catch (error) {
            this.log.warn(`Discarding malformed signaling payload: ${raw}`);
            return;
        }
        const { type } = message;
        if (!type) {
            this.log.warn('Received signaling message without type');
            return;
        }
        switch (type) {
            case 'WHO_HAS_REPLY':
                this.handleWhoHasReply(message);
                break;
            case 'REPORT_ACK':
                this.emit('report_ack', message);
                break;
            case 'peer_list':
                this.emit('peer_list', message);
                break;
            // WebRTC signaling passthrough
            case 'RTC_OFFER':
            case 'RTC_ANSWER':
            case 'ICE_CANDIDATE':
                this.emit(type, message);
                break;
            case 'ERROR':
            case 'error':
                this.log.error(`Signaling error: ${message.message || 'unknown error'}`);
                break;
            default:
                this.emit('message', message);
        }
    }

    handleWhoHasReply(message) {
        const segmentId = message.segmentId;
        if (!segmentId) {
            this.log.warn('WHO_HAS_REPLY missing segmentId');
            return;
        }
        const pending = this.pendingWhoHas.get(segmentId);
        if (!pending) {
            this.log.debug(`No pending WHO_HAS for segment ${segmentId}`);
            return;
        }
        clearTimeout(pending.timer);
        this.pendingWhoHas.delete(segmentId);
        pending.resolve(Array.isArray(message.peers) ? message.peers : []);
    }

    cleanupPending(error) {
        for (const [segmentId, pending] of this.pendingWhoHas.entries()) {
            clearTimeout(pending.timer);
            pending.reject?.(error);
        }
        this.pendingWhoHas.clear();
    }

    async close() {
        if (this.socket) {
            this.socket.close();
        }
        this.connected = false;
    }

    // --- WebRTC signaling helpers ---
    send(message) {
        if (!this.connected || !this.socket) {
            throw new Error('Signaling connection is not ready');
        }
        const payload = JSON.stringify(message);
        this.socket.send(payload);
    }

    sendRtcOffer(to, sdp) {
        this.send({ type: 'RTC_OFFER', from: this.clientId, to, streamId: this.streamId, sdp });
    }

    sendRtcAnswer(to, sdp) {
        this.send({ type: 'RTC_ANSWER', from: this.clientId, to, streamId: this.streamId, sdp });
    }

    sendIceCandidate(to, candidate) {
        this.send({ type: 'ICE_CANDIDATE', from: this.clientId, to, streamId: this.streamId, candidate });
    }
}

module.exports = SignalingClient;
