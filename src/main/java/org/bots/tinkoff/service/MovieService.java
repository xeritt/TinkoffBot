package org.bots.tinkoff.service;
import java.util.ArrayList;
import java.util.List;

import org.bots.tinkoff.model.Movie;
import org.bots.tinkoff.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovieService implements IMovieService {

    @Autowired
    MovieRepository movieRepository;

    @Override
    public List<Movie> getAllMovies() {
        List<Movie> movies = new ArrayList<Movie>();
        movieRepository.findAll().forEach(movie -> movies.add(movie));
        return movies;
    }

    @Override
    public Movie getMovieById(int id) {
        return movieRepository.findById(id).get();
    }

    @Override
    public void saveOrUpdate(Movie mvoie) {
    	movieRepository.save(mvoie);
    }

    @Override
    public void delete(int id) {
    	movieRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Movie createMovie(Movie movie){
        return movieRepository.save(movie);
    }

}