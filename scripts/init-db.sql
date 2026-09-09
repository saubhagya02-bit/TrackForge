-- Create separate databases for each microservice
CREATE DATABASE trackforge_auth;
CREATE DATABASE trackforge_bugs;
CREATE DATABASE trackforge_notifications;
CREATE DATABASE trackforge_analytics;

-- Enable pgvector in bug database for AI duplicate detection
\c trackforge_bugs
CREATE EXTENSION IF NOT EXISTS vector;

-- Enable pgvector in auth database
\c trackforge_auth
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";