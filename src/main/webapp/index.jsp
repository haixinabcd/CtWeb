<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>图文卡片示例</title>
    <style>
        body {
            font-family: 'Arial', sans-serif;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            background-color: #f5f5f5;
            margin: 0;
            padding: 20px;
        }
        .card {
            width: 300px;
            background: white;
            border-radius: 10px;
            box-shadow: 0 4px 8px rgba(0,0,0,0.1);
            overflow: hidden;
            transition: transform 0.3s ease;
        }
        .card:hover {
            transform: translateY(-5px);
        }
        .card-image {
            width: 100%;
            height: 200px;
            object-fit: cover;
        }
        .card-content {
            padding: 15px;
        }
        .card-title {
            font-size: 18px;
            margin: 0 0 10px 0;
            color: #333;
        }
        .card-link {
            display: inline-block;
            padding: 8px 15px;
            background-color: #4285f4;
            color: white;
            text-decoration: none;
            border-radius: 5px;
            font-size: 14px;
            transition: background-color 0.3s;
        }
        .card-link:hover {
            background-color: #3367d6;
        }
    </style>
</head>
<body>
    <div class="card">
        <img src="img/378-300x200.jpg" alt="自然风景图片" class="card-image">
        <div class="card-content">
            <h3 class="card-title">探索大自然的美丽</h3>
            <a href="https://www.example.com" class="card-link">了解更多</a>
        </div>
    </div>
</body>
</html>

