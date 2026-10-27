/** CODE128-B。条码内容只编码传入的条码编号。 */
const CODE128_PATTERNS = [
  '212222', '222122', '222221', '121223', '121322', '131222', '122213', '122312', '132212', '221213',
  '221312', '231212', '112232', '122132', '122231', '113222', '123122', '123221', '223211', '221132',
  '221231', '213212', '223112', '312131', '311222', '321122', '321221', '312212', '322112', '322211',
  '212123', '212321', '232121', '111323', '131123', '131321', '112313', '132113', '132311', '211313',
  '231113', '231311', '112133', '112331', '132131', '113123', '113321', '133121', '313121', '211331',
  '231131', '213113', '213311', '213131', '311123', '311321', '331121', '312113', '312311', '332111',
  '314111', '221411', '431111', '111224', '111422', '121124', '121421', '141122', '141221', '112214',
  '112412', '122114', '122411', '142112', '142211', '241211', '221114', '413111', '241112', '134111',
  '111242', '121142', '121241', '114212', '124112', '124211', '411212', '421112', '421211', '212141',
  '214121', '412121', '111143', '111341', '131141', '114113', '114311', '411113', '411311', '113141',
  '114131', '311141', '411131', '211412', '211214', '211232', '2331112',
];

const START_B = 104;
const STOP = 106;

function encodeCode128B(text: string) {
  const codes = [START_B];
  for (const char of text) {
    const value = char.charCodeAt(0) - 32;
    if (value < 0 || value > 95) {
      throw new Error(`条码编号包含不支持的字符：${char}`);
    }
    codes.push(value);
  }
  let checksum = codes[0];
  for (let index = 1; index < codes.length; index += 1) {
    checksum += codes[index] * index;
  }
  codes.push(checksum % 103);
  codes.push(STOP);
  return codes;
}

function patternWidth(pattern: string) {
  return pattern.split('').reduce((sum, part) => sum + Number(part), 0);
}

export function isCode128Text(text: string) {
  return [...text].every((char) => {
    const value = char.charCodeAt(0) - 32;
    return value >= 0 && value <= 95;
  });
}

export function code128DataUrl(text: string, barWidth = 2, height = 72) {
  const codes = encodeCode128B(text);
  const quiet = 10;
  const modules = codes.reduce((sum, code) => sum + patternWidth(CODE128_PATTERNS[code]), 0) + quiet * 2;
  const canvas = document.createElement('canvas');
  canvas.width = modules * barWidth;
  canvas.height = height;
  const context = canvas.getContext('2d');
  if (!context) {
    return '';
  }
  context.fillStyle = '#ffffff';
  context.fillRect(0, 0, canvas.width, canvas.height);
  context.fillStyle = '#000000';
  let x = quiet * barWidth;
  codes.forEach((code) => {
    let drawBar = true;
    CODE128_PATTERNS[code].split('').forEach((part) => {
      const width = Number(part) * barWidth;
      if (drawBar) {
        context.fillRect(x, 0, width, height);
      }
      x += width;
      drawBar = !drawBar;
    });
  });
  return canvas.toDataURL('image/png');
}
