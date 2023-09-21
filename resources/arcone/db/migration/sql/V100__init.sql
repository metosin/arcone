--
-- rockstore schema:
--

-- Make sure we have pgcrypto installed:

create schema if not exists crypto;
create extension if not exists pgcrypto schema crypto;

-- Tighten security:

revoke create on schema public from public;


-- Log notice messages:

update pg_settings set setting = 'notice' where name = 'log_min_messages';
select set_config('log_min_messages', 'notice', false);
