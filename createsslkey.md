# 🔐 SSL Certificate Setup Guide

Hướng dẫn tạo SSL certificates cho dự án XOXO Backend để chạy HTTPS trên localhost.

---

## �� Prerequisites

- mkcert (để tạo SSL certificates)
- Java 17+ (đã có sẵn trong project)

---

## 🚀 Quick Setup

### **Bước 1: Cài đặt mkcert**

#### **Windows:**
```powershell
# Cài đặt Chocolatey (nếu chưa có)
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# Cài đặt mkcert
choco install mkcert

# Hoặc download trực tiếp từ GitHub
# https://github.com/FiloSottile/mkcert/releases
```

#### **macOS:**
```bash
# Cài đặt mkcert
brew install mkcert
```

#### **Linux (Ubuntu/Debian):**
```bash
# Cài đặt dependencies
sudo apt install libnss3-tools

# Download mkcert
wget -O mkcert https://github.com/FiloSottile/mkcert/releases/download/v1.4.4/mkcert-v1.4.4-linux-amd64
chmod +x mkcert
sudo mv mkcert /usr/local/bin/
```

### **Bước 2: Tạo CA Local**

```bash
# Tạo Certificate Authority (CA) local
mkcert -install
```

**Kết quả**: mkcert sẽ tạo một CA local và cài đặt vào hệ thống, cho phép browser tin tưởng certificates được tạo bởi mkcert.

### **Bước 3: Tạo SSL Certificates**

```bash
# Tạo chứng chỉ cho localhost
mkcert localhost 127.0.0.1 ::1
```

**Kết quả**: Tạo ra 2 files:
- `localhost+2.pem` (certificate)
- `localhost+2-key.pem` (private key)

### **Bước 4: Di chuyển Certificates vào Project**

#### **Windows:**
```powershell
# Tạo thư mục certificates
mkdir src\main\resources\certificates

# Di chuyển certificates
move localhost+2.pem src\main\resources\certificates\
move localhost+2-key.pem src\main\resources\certificates\
```

#### **macOS/Linux:**
```bash
# Tạo thư mục certificates
mkdir -p src/main/resources/certificates

# Di chuyển certificates
mv localhost+2.pem src/main/resources/certificates/
mv localhost+2-key.pem src/main/resources/certificates/
```
choco install openssl

refreshenv

& "C:\Program Files\OpenSSL-Win64\bin\openssl.exe" pkcs12 -export -in localhost+2.pem -inkey localhost+2-key.pem -out localhost.p12 -name localhost -password pass:tuan123
