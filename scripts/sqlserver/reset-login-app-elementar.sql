/*
  Execute no SSMS ligado como sa (ou outro administrador da instancia).
  1) Substitua TROQUE_ESTA_SENHA pela mesma senha que vai pôr em RESSARCIMENTO_DB_PASSWORD no ressarcimento.env
  2) Ajuste o nome da base se nao for ressarcimento
*/
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'app_elementar')
    CREATE LOGIN [app_elementar] WITH PASSWORD = N'TROQUE_ESTA_SENHA', CHECK_POLICY = OFF;
ELSE
    ALTER LOGIN [app_elementar] WITH PASSWORD = N'TROQUE_ESTA_SENHA', CHECK_POLICY = OFF;
GO

ALTER LOGIN [app_elementar] ENABLE;
GO

IF DB_ID(N'ressarcimento') IS NULL
    CREATE DATABASE ressarcimento;
GO

USE [ressarcimento];
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'app_elementar')
    CREATE USER [app_elementar] FOR LOGIN [app_elementar];
ALTER ROLE db_owner ADD MEMBER [app_elementar];
GO
