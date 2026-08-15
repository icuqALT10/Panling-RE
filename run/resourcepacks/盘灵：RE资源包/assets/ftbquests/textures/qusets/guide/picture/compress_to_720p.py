import os
from PIL import Image

# 目标高度（720p 标准）
TARGET_HEIGHT = 720
OUTPUT_DIR = "new_picture"

def main():
    # 创建输出文件夹
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    # 获取当前目录下所有 .png 文件（不区分大小写）
    png_files = [f for f in os.listdir('.') if f.lower().endswith('.png')]
    if not png_files:
        print("当前目录下没有找到 .png 文件。")
        return

    print(f"找到 {len(png_files)} 个 PNG 文件，开始处理...")

    for filename in png_files:
        try:
            img = Image.open(filename)
            w, h = img.size

            # 计算新宽度（保持宽高比）
            new_w = int(w * TARGET_HEIGHT / h)
            new_size = (new_w, TARGET_HEIGHT)

            # 使用 LANCZOS 重采样获得高质量缩放
            resized = img.resize(new_size, Image.Resampling.LANCZOS)

            # 保存到输出目录，保持原文件名
            out_path = os.path.join(OUTPUT_DIR, filename)
            resized.save(out_path, format='PNG', optimize=True)
            print(f"✅ {filename} -> {new_size}  (原尺寸 {w}x{h})")

        except Exception as e:
            print(f"❌ 处理 {filename} 时出错: {e}")

    print(f"\n所有图片处理完成！结果保存在 '{OUTPUT_DIR}' 文件夹中。")

if __name__ == "__main__":
    main()