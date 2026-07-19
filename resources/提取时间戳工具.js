const fs = require('fs');

const inputPath = "D:/Minecraft/mod_projects/panlingre-1.21.1/src/main/resources/assets/panlingre/animations/entity/boss/pangu.animation.json";
const outputPath = "D:/Minecraft/mod_projects/panlingre-1.21.1/resources/timeline_tick.txt";

const data = JSON.parse(fs.readFileSync(inputPath, 'utf-8'));
const animations = data.animations || {};

const lines = [];
lines.push("动画指令关键帧 (秒 -> tick)");
lines.push("公式: tick = 秒 x 20");
lines.push("=".repeat(60));
lines.push("");

let totalAnim = 0;
let totalCmd = 0;

for (const [animName, animData] of Object.entries(animations)) {
    const timeline = animData.timeline;
    if (!timeline || typeof timeline !== 'object') continue;

    const keys = Object.keys(timeline).filter(k => !isNaN(parseFloat(k)));
    if (keys.length === 0) continue;

    totalAnim++;
    totalCmd += keys.length;

    // 按秒数排序
    keys.sort((a, b) => parseFloat(a) - parseFloat(b));

    lines.push(`"${animName}": {`);
    lines.push(`  "timeline": {`);
    for (const k of keys) {
        const sec = parseFloat(k);
        const tick = Math.round(sec * 20);
        const cmds = timeline[k];
        lines.push(`    "${tick}": "${cmds}";`);
    }
    lines.push(`  }`);
    lines.push(`}`);
    lines.push("");
}

const header = `共 ${totalAnim} 个动画, ${totalCmd} 条指令\n`;
lines.unshift(header);

fs.writeFileSync(outputPath, lines.join("\n"), 'utf-8');
console.log(`Done! ${totalAnim} animations with timeline, ${totalCmd} total instructions`);
console.log(`Output: ${outputPath}`);
