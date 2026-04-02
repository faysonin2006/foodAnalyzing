SELECT 'CREATE DATABASE auth'
WHERE NOT EXISTS (
  SELECT FROM pg_database WHERE datname = 'auth'
)\gexec

SELECT 'CREATE DATABASE userprofiles'
WHERE NOT EXISTS (
  SELECT FROM pg_database WHERE datname = 'userprofiles'
)\gexec

SELECT 'CREATE DATABASE foodanalysis'
WHERE NOT EXISTS (
  SELECT FROM pg_database WHERE datname = 'foodanalysis'
)\gexec

SELECT 'CREATE DATABASE recipesspoonacular'
WHERE NOT EXISTS (
  SELECT FROM pg_database WHERE datname = 'recipesspoonacular'
)\gexec

SELECT 'CREATE DATABASE recipes'
WHERE NOT EXISTS (
  SELECT FROM pg_database WHERE datname = 'recipes'
)\gexec

SELECT 'CREATE DATABASE households'
WHERE NOT EXISTS (
  SELECT FROM pg_database WHERE datname = 'households'
)\gexec
