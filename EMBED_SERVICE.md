# BGE Embedding 服务

基于 BAAI/bge-large-zh-v1.5 的独立 Embedding 服务，为 wy-rag（无忧 RAG 知识库）提供文本向量化能力。

## 架构位置

```
┌──────────────┐     POST /embed      ┌─────────────────┐
│  rag-bootstrap │ ──────────────────> │  Embed Service   │
│  (SpringBoot) │ <────────────────── │  (FastAPI:5001)  │
└──────────────┘     embeddings[]      └─────────────────┘
                                             │
                                        BGE-large-zh-v1.5
                                        (sentence-transformers)
```

SpringBoot 后端通过 `EmbeddingService` 调用本服务的 `/embed` 接口，将文本转为 1024 维向量，存入 Milvus 向量库供语义检索。

## 模型信息

| 项目 | 说明 |
|------|------|
| 模型 | BAAI/bge-large-zh-v1.5 |
| 维度 | 1024 |
| 最大长度 | 512 tokens |
| 优化 | FP16 (CUDA) / FP32 (CPU) |

## 环境要求

- Python 3.8+
- CUDA（可选，有 GPU 则自动启用 FP16 加速）

## 部署步骤

### 1. 配置 pip 镜像（国内）

```bash
pip3 config set global.index-url https://pypi.mirrors.ustc.edu.cn/simple
pip3 config set install.trusted-host pypi.mirrors.ustc.edu.cn
```

### 2. 安装依赖

```bash
pip install sentence-transformers fastapi uvicorn
```

### 3. 下载模型（可选，提前缓存）

```bash
export HF_ENDPOINT=https://hf-mirror.com

pip install -U huggingface_hub
huggingface-cli download BAAI/bge-large-zh-v1.5 --local-dir ./bge-large-zh-v1.5
```

模型下载到本地后，修改 `app.py` 中的模型路径为本地目录即可离线使用。

### 4. 启动服务

```bash
export HF_ENDPOINT=https://hf-mirror.com
uvicorn app:app --host 0.0.0.0 --port 5001
```

生产环境建议：

```bash
uvicorn app:app --host 0.0.0.0 --port 5001 --workers 2 --log-level info
```

### 5. 验证

```bash
# 健康检查
curl http://localhost:5001/health

# 向量化测试
curl -X POST http://localhost:5001/embed \
  -H "Content-Type: application/json" \
  -d '{
    "texts": ["你好", "RAG是什么"]
  }'
```

预期返回：

```json
{
  "embeddings": [[0.0123, -0.0456, ...], [0.0789, -0.0321, ...]],
  "dimensions": 1024
}
```

## API

### `GET /health`

健康检查。

**响应：**
```json
{"status": "ok"}
```

### `POST /embed`

文本向量化。

**请求：**
```json
{
  "texts": ["文本1", "文本2"]
}
```

**限制：** 单次最多 128 条文本。

**响应：**
```json
{
  "embeddings": [[...], [...]],
  "dimensions": 1024
}
```

## 与主项目集成

在 SpringBoot 配置中指定本服务地址：

```yaml
# application-dev.yml
embedding:
  api_url: http://localhost:5001/embed
  model_name: bge-large-zh-v1.5
```

`rag-service` 中的 `EmbeddingService` 会通过 HTTP 调用本服务，完成文本到向量的转换后存入 Milvus。

## 性能参考

| 硬件 | batch_size=32 | texts=128 |
|------|---------------|-----------|
| NVIDIA T4 | ~0.3s | ~1.2s |
| CPU (8核) | ~1.5s | ~6s |
| Apple M1 | ~0.8s | ~3s |
