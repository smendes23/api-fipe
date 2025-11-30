#!/bin/bash

echo "==================================="
echo "Inicializando banco de dados OAuth"
echo "==================================="

# Verificar se o Docker está instalado
if ! command -v docker &> /dev/null; then
    echo "❌ Docker não está instalado. Por favor, instale o Docker primeiro."
    exit 1
fi

# Verificar se o Docker Compose está instalado
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose não está instalado. Por favor, instale o Docker Compose primeiro."
    exit 1
fi

# Iniciar o PostgreSQL
echo "🚀 Iniciando PostgreSQL..."
docker-compose up -d

# Aguardar o PostgreSQL ficar pronto
echo "⏳ Aguardando PostgreSQL ficar pronto..."
sleep 10

# Verificar se o banco está acessível
echo "🔍 Verificando conexão com o banco..."
docker exec oauth-postgres pg_isready -U oauth_user -d oauth_db

if [ $? -eq 0 ]; then
    echo "✅ PostgreSQL está pronto!"
    echo ""
    echo "Informações de conexão:"
    echo "  Host: localhost"
    echo "  Porta: 5432"
    echo "  Database: oauth_db"
    echo "  Username: oauth_user"
    echo "  Password: oauth_pass"
    echo ""
    echo "As migrations do Flyway serão executadas automaticamente quando o OAuth Server iniciar."
else
    echo "❌ Erro ao conectar ao PostgreSQL"
    exit 1
fi
