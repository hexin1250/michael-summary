CREATE DATABASE IF NOT EXISTS investment DEFAULT CHARACTER SET utf8;
-- Gives all the privileges to the new user on the newly created database
grant all on investment.* to 'springuser'@'%';
