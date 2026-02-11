package com.demo.library;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/books")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookResource {

    @GET
    public List<Book> getAll() {
        return Book.listAll();
    }

    @GET
    @Path("/{id}")
    public Book getById(@PathParam("id") Long id) {
        Book book = Book.findById(id);
        if (book == null) {
            throw new WebApplicationException("Book not found with id: " + id, Response.Status.NOT_FOUND);
        }
        return book;
    }

    @POST
    @Transactional
    public Response create(Book book) {
        book.persist();
        return Response.status(Response.Status.CREATED).entity(book).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Book update(@PathParam("id") Long id, Book bookData) {
        Book book = Book.findById(id);
        if (book == null) {
            throw new WebApplicationException("Book not found with id: " + id, Response.Status.NOT_FOUND);
        }
        book.title = bookData.title;
        book.author = bookData.author;
        book.publicationYear = bookData.publicationYear;
        book.isbn = bookData.isbn;
        return book;
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        Book book = Book.findById(id);
        if (book == null) {
            throw new WebApplicationException("Book not found with id: " + id, Response.Status.NOT_FOUND);
        }
        book.delete();
        return Response.noContent().build();
    }
}
