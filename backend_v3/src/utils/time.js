/**
 * Centralized West Africa Time (WAT) handling for Pikop.
 * Nigeria is UTC+1, no DST.
 */

/**
 * Returns current Date in WAT.
 */
const getWATDate = () => {
    const now = new Date();
    // Offset for UTC+1
    const watOffset = 1 * 60 * 60 * 1000;
    return new Date(now.getTime() + watOffset);
};

/**
 * Returns current HH:mm string in WAT.
 */
const getWATTimeStr = () => {
    const d = getWATDate();
    return `${d.getUTCHours().toString().padStart(2, '0')}:${d.getUTCMinutes().toString().padStart(2, '0')}`;
};

/**
 * Checks if current WAT time is within an [open, close] window.
 * Format: "HH:mm"
 */
const isWithinWindow = (nowTime, start, end) => {
    if (start <= end) {
        return nowTime >= start && nowTime <= end;
    } else {
        // Over-midnight window (e.g. 22:00 to 02:00)
        return nowTime >= start || nowTime <= end;
    }
};

module.exports = {
    getWATDate,
    getWATTimeStr,
    isWithinWindow
};