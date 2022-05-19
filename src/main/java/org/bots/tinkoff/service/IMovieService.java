package org.bots.tinkoff.service;

import org.bots.tinkoff.model.Movie;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IMovieService {
    List<Movie> getAllMovies();

    Movie getMovieById(int id);

    @Transactional
    void saveOrUpdate(Movie mvoie);

    @Transactional
    void delete(int id);

    @Transactional
    Movie createMovie(Movie movie);
}
