const wrtc = require('wrtc');
const { EventEmitter } = require('events');
const {
    FRAME_CHUNK,
    FRAME_DONE,
    FRAME_REQUEST,
    DEFAULT_CHUNK_SIZE,
    buildChunkFrame,
    buildDoneFrame,
    buildRequestFrame,
    parseFrame
} = require('./chunk-protocol');

class WebRtcConnectionManager extends EventEmitter {
    constructor({ clientId, streamId, signaling, stunServers, logger, chunkSize = DEFAULT_CHUNK_SIZE }) {
        super();
        this.clientId = clientId;
        this.streamId = streamId;
        this.signaling = signaling;
        this.log = logger;
        this.chunkSize = chunkSize;
        this.iceServers = (stunServers || [
            'stun:stun.l.google.com:19302',
            'stun:global.stun.twilio.com:3478'
        ]).map((u) => ({ urls: u }));
        this.peers = new Map(); // peerId -> { pc, dc }

        this._wireSignaling();
    }

    _wireSignaling() {
        this.signaling.on('peer_list', (msg) => {
            if (!Array.isArray(msg.peers)) return;
            msg.peers.filter((pid) => pid !== this.clientId).forEach((pid) => this.ensureOffer(pid));
        });

        this.signaling.on('rtcOffer', async (msg) => {
            const from = msg.from;
            if (!from || from === this.clientId) return;
            const { pc } = this._ensurePeer(from, false);
            try {
                await pc.setRemoteDescription({ type: 'offer', sdp: msg.sdp });
                const answer = await pc.createAnswer();
                await pc.setLocalDescription(answer);
                this.signaling.sendRtcAnswer(from, answer.sdp);
                this.log.info(`RTC: answer -> ${from}`);
            } catch (e) {
                this.log.error(`RTC offer fail from ${from}: ${e.message}`);
            }
        });

        this.signaling.on('rtcAnswer', async (msg) => {
            const from = msg.from;
            if (!from || from === this.clientId) return;
            const peer = this.peers.get(from);
            if (!peer) return;
            try {
                await peer.pc.setRemoteDescription({ type: 'answer', sdp: msg.sdp });
            } catch (e) {
                this.log.warn(`RTC answer set fail from ${from}: ${e.message}`);
            }
        });

        this.signaling.on('iceCandidate', async (msg) => {
            const from = msg.from;
            if (!from || from === this.clientId) return;
            const peer = this.peers.get(from);
            if (!peer) return;
            try {
                await peer.pc.addIceCandidate(msg.candidate);
            } catch (e) {
                this.log.warn(`ICE add fail from ${from}: ${e.message}`);
            }
        });
    }

    _ensurePeer(remotePeerId, isInitiator) {
        if (this.peers.has(remotePeerId)) return this.peers.get(remotePeerId);
        const pc = new wrtc.RTCPeerConnection({ iceServers: this.iceServers });
        const rec = { pc, dc: null, id: remotePeerId };
        this.peers.set(remotePeerId, rec);

        pc.onicecandidate = (ev) => {
            if (ev.candidate) this.signaling.sendIceCandidate(remotePeerId, ev.candidate);
        };
        pc.onconnectionstatechange = () => {
            this.log.info(`RTC(${remotePeerId})=${pc.connectionState}`);
            if (['failed', 'disconnected', 'closed'].includes(pc.connectionState)) {
                this.peers.delete(remotePeerId);
            }
        };
        pc.ondatachannel = (ev) => this._setupChannel(remotePeerId, ev.channel);
        if (isInitiator) this._setupChannel(remotePeerId, pc.createDataChannel('segments'));
        return rec;
    }

    _setupChannel(remotePeerId, channel) {
        const rec = this.peers.get(remotePeerId);
        if (rec) rec.dc = channel;
        const assembly = new Map(); // segmentId -> { total, chunks, received }

        channel.onopen = () => this.log.success(`DC open -> ${remotePeerId}`);
        channel.onclose = () => this.log.warn(`DC close -> ${remotePeerId}`);
        channel.onerror = (e) => this.log.error(`DC error -> ${remotePeerId}: ${e.message}`);
        channel.onmessage = (ev) => {
            try {
                const data = ev.data;
                if (Buffer.isBuffer(data) || data instanceof ArrayBuffer) {
                    const frame = parseFrame(data);
                    if (frame.type === FRAME_CHUNK) {
                        let st = assembly.get(frame.segmentId);
                        if (!st) { st = { total: frame.total, chunks: new Array(frame.total), received: 0 }; assembly.set(frame.segmentId, st); }
                        if (!st.chunks[frame.index]) {
                            st.chunks[frame.index] = Buffer.from(frame.payload);
                            st.received += 1;
                        }
                        if (st.received === st.total) {
                            const full = Buffer.concat(st.chunks);
                            assembly.delete(frame.segmentId);
                            this.emit('segment', { from: remotePeerId, segmentId: frame.segmentId, data: full });
                        }
                    } else if (frame.type === FRAME_DONE) {
                        // optional
                    } else if (frame.type === FRAME_REQUEST) {
                        this.emit('request', { from: remotePeerId, segmentId: frame.segmentId, channel });
                    }
                } else {
                    const msg = JSON.parse(data.toString());
                    if (msg.type === 'SEGMENT_REQUEST' && msg.segmentId) {
                        // backward compat: convert to binary request
                        try { channel.send(buildRequestFrame(msg.segmentId)); } catch { }
                    }
                }
            } catch (e) {
                this.log.warn(`DC message fail from ${remotePeerId}: ${e.message}`);
            }
        };
    }

    async ensureOffer(remotePeerId) {
        const { pc } = this._ensurePeer(remotePeerId, true);
        try {
            const offer = await pc.createOffer();
            await pc.setLocalDescription(offer);
            this.signaling.sendRtcOffer(remotePeerId, offer.sdp);
        } catch (e) {
            this.log.error(`Create/send offer fail -> ${remotePeerId}: ${e.message}`);
        }
    }

    requestSegment(segmentId) {
        for (const [, rec] of this.peers) {
            if (rec.dc && rec.dc.readyState === 'open') {
                try { rec.dc.send(buildRequestFrame(segmentId)); } catch { }
            }
        }
    }

    sendSegment(channel, segmentId, buffer) {
        const total = Math.ceil(buffer.length / this.chunkSize);
        for (let i = 0; i < total; i++) {
            const start = i * this.chunkSize;
            const end = Math.min(buffer.length, start + this.chunkSize);
            const payload = buffer.subarray(start, end);
            if (channel.bufferedAmount > 512 * 1024) {
                // backpressure
                // node-wrtc doesn't have addEventListener, emulate with onbufferedamountlow if available
            }
            try { channel.send(buildChunkFrame(segmentId, i, total, payload)); } catch (e) { break; }
        }
        try { channel.send(buildDoneFrame(segmentId)); } catch { }
    }
}

module.exports = WebRtcConnectionManager;
