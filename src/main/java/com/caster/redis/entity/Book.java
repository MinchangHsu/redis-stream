package com.caster.redis.entity;

import com.github.javafaker.Faker;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Locale;

/**
 * Author: Minchang Hsu (Caster)
 * Date: 2022/12/30
 */
@Getter
@Setter
@ToString
public class Book {
    private String title;
    private String author;
    
    public static Book create() {
        Faker faker = new Faker(Locale.JAPAN);
        com.github.javafaker.Book fakerBook = faker.book();
        Book book = new Book();
        book.setTitle(fakerBook.title());
        book.setAuthor(fakerBook.author());
        return book;
    }
}
