package de.dhge.resapi.service;

import java.util.ArrayList;

import org.springframework.data.jpa.repository.JpaRepository;

import de.dhge.resapi.model.TodoEntry;

public interface TodoEntryRepository extends JpaRepository<TodoEntry, Long>{

	public TodoEntry findByOid(Long oid);
	
	public ArrayList<TodoEntry> findAll();
	
	public ArrayList<TodoEntry> findByEntryTitle(String entryTitle);
		
}
