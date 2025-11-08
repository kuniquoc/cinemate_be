class PlaybackBuffer {
    constructor() {
        this.queue = [];
    }

    push(item) {
        this.queue.push(item);
    }

    shift() {
        return this.queue.shift();
    }

    size() {
        return this.queue.length;
    }

    clear() {
        this.queue.length = 0;
    }

    isEmpty() {
        return this.queue.length === 0;
    }
}

module.exports = PlaybackBuffer;
