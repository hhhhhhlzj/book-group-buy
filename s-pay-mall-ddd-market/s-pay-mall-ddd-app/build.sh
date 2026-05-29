# 普通镜像构建，随系统版本构建 amd/arm
docker build -t s-pay-mall-ddd-app:2.0 -f ./Dockerfile .

# 兼容 amd、arm 构建镜像（推送时把 your-namespace 替换成你自己的镜像仓库命名空间）
# docker buildx build --load --platform linux/amd64,linux/arm64 -t your-namespace/s-pay-mall-ddd-app:1.0 -f ./Dockerfile . --push
