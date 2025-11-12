// Binary chunk wire format
// Types
const FRAME_CHUNK = 0x01;    // [1:type][1:idLen][id][2:index][2:total][payload]
const FRAME_DONE = 0x02;     // [1:type][1:idLen][id]
const FRAME_REQUEST = 0x03;  // [1:type][1:idLen][id]

const DEFAULT_CHUNK_SIZE = 32 * 1024; // 32KB

function buildChunkFrame(segmentId, index, total, payload) {
    const idBytes = Buffer.from(segmentId, 'utf8');
    if (idBytes.length > 255) throw new Error('segmentId too long');
    const header = Buffer.allocUnsafe(1 + 1 + idBytes.length + 2 + 2);
    let o = 0;
    header.writeUInt8(FRAME_CHUNK, o); o += 1;
    header.writeUInt8(idBytes.length, o); o += 1;
    idBytes.copy(header, o); o += idBytes.length;
    header.writeUInt16BE(index, o); o += 2;
    header.writeUInt16BE(total, o); o += 2;
    return Buffer.concat([header, payload]);
}

function buildDoneFrame(segmentId) {
    const idBytes = Buffer.from(segmentId, 'utf8');
    if (idBytes.length > 255) throw new Error('segmentId too long');
    const buf = Buffer.allocUnsafe(1 + 1 + idBytes.length);
    let o = 0;
    buf.writeUInt8(FRAME_DONE, o); o += 1;
    buf.writeUInt8(idBytes.length, o); o += 1;
    idBytes.copy(buf, o);
    return buf;
}

function buildRequestFrame(segmentId) {
    const idBytes = Buffer.from(segmentId, 'utf8');
    if (idBytes.length > 255) throw new Error('segmentId too long');
    const buf = Buffer.allocUnsafe(1 + 1 + idBytes.length);
    let o = 0;
    buf.writeUInt8(FRAME_REQUEST, o); o += 1;
    buf.writeUInt8(idBytes.length, o); o += 1;
    idBytes.copy(buf, o);
    return buf;
}

function parseFrame(bufferLike) {
    const b = Buffer.isBuffer(bufferLike) ? bufferLike : Buffer.from(bufferLike);
    let o = 0;
    const type = b.readUInt8(o); o += 1;
    const idLen = b.readUInt8(o); o += 1;
    const segmentId = b.subarray(o, o + idLen).toString('utf8'); o += idLen;
    if (type === FRAME_CHUNK) {
        const index = b.readUInt16BE(o); o += 2;
        const total = b.readUInt16BE(o); o += 2;
        const payload = b.subarray(o);
        return { type, segmentId, index, total, payload };
    }
    return { type, segmentId };
}

module.exports = {
    FRAME_CHUNK,
    FRAME_DONE,
    FRAME_REQUEST,
    DEFAULT_CHUNK_SIZE,
    buildChunkFrame,
    buildDoneFrame,
    buildRequestFrame,
    parseFrame
};
