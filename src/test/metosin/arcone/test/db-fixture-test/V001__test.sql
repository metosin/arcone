drop role if exists ${test_username};
create role ${test_username} noinherit login password '${test_password}';


drop role if exists monkey;
create role monkey noinherit nologin;
grant monkey to ${test_username};


grant usage on schema test to monkey;


create table test.test_data (
  id integer primary key
);


grant select, insert on table test.test_data to monkey;


create function test.who_am_i()
  returns table(session_user_role text, current_user_role text)
  language sql
  stable
  as $$
    select 
      session_user         as session_user_role,
      current_user         as current_user_role;
  $$;
