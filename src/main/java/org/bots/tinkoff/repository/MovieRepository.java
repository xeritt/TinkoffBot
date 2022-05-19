	package org.bots.tinkoff.repository;
	
	import org.bots.tinkoff.model.Movie;
	import org.springframework.data.repository.CrudRepository;
	
	public interface MovieRepository extends CrudRepository<Movie, Integer> {}