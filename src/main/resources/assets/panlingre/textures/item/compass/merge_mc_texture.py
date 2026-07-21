#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import glob
from PIL import Image

# 支持的图片扩展名
SUPPORTED_EXT = ('.png', '.jpg', '.jpeg', '.bmp', '.gif', '.tiff')

def main():
    # 获取当前目录
    cwd = os.getcwd()
    
    # 获取所有图片文件，按文件名排序
    files = []
    for ext in SUPPORTED_EXT:
        files.extend(glob.glob(os.path.join(cwd, '*' + ext)))
        files.extend(glob.glob(os.path.join(cwd, '*' + ext.upper())))
    
    # 去重并排序（按文件名）
    files = sorted(set(files), key=os.path.basename)
    
    if not files:
        print("错误：当前目录下未找到任何图片文件。")
        return
    
    print(f"找到 {len(files)} 个图片文件：")
    for f in files:
        print(f"  {os.path.basename(f)}")
    
    # 读取所有图片，并检查尺寸（第一张作为参考）
    images = []
    target_size = None
    for f in files:
        try:
            img = Image.open(f).convert('RGBA')  # 统一转为 RGBA 便于合并
        except Exception as e:
            print(f"警告：无法读取 {os.path.basename(f)}，跳过。错误：{e}")
            continue
        
        # 如果第一张图片尺寸未知，则设定为目标尺寸；否则检查是否一致
        if target_size is None:
            target_size = img.size
            print(f"参考尺寸：{target_size[0]} x {target_size[1]}")
        elif img.size != target_size:
            print(f"警告：{os.path.basename(f)} 尺寸 {img.size} 与参考尺寸不符，将强制缩放至 {target_size}")
            img = img.resize(target_size, Image.Resampling.LANCZOS)
        
        images.append(img)
    
    if not images:
        print("错误：没有成功读取任何图片。")
        return
    
    # 计算最终图像尺寸（宽度不变，高度 = 单张高度 × 数量）
    width, height = target_size
    total_height = height * len(images)
    print(f"生成图像尺寸：{width} x {total_height}")
    
    # 创建空白画布
    combined = Image.new('RGBA', (width, total_height))
    
    # 垂直拼接
    for i, img in enumerate(images):
        y_offset = i * height
        combined.paste(img, (0, y_offset))
    
    # 保存结果
    output_path = os.path.join(cwd, 'output.png')
    combined.save(output_path, 'PNG')
    print(f"合并完成！输出文件：{output_path}")

if __name__ == '__main__':
    main()