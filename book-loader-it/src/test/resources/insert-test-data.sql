DROP TABLE IF EXISTS books;

CREATE TABLE books (
  id SERIAL PRIMARY KEY,
  title TEXT,
  author TEXT
);

INSERT INTO books (title, author) VALUES
  ('The Lord of the Rings', 'J. R. R. Tolkien'),
  ('Dracula', 'Bram Stoker'),
  ('The Wonderful Wizard of Oz', 'L. Frank Baum');