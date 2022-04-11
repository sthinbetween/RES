package de.dhge.resapi.service.impl;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.dhge.resapi.model.TodoEntry;
import de.dhge.resapi.service.TodoEntryRepository;

@Service
public class TodoEntryService {
	
	@Autowired
	TodoEntryRepository todoEntryRepository;

	public TodoEntry findByOid(Long oid) {
		return  todoEntryRepository.findByOid(oid);
	}
	
	public ArrayList<TodoEntry> findAll(){
		return todoEntryRepository.findAll();
	}
	
	public ArrayList<TodoEntry> findByEntryTitle(String entryTitle){
		return todoEntryRepository.findByEntryTitle(entryTitle);
	}
	
	public ArrayList<TodoEntry> deleteByTitle(String entryTitle) {
		
		ArrayList<TodoEntry> toDelete = todoEntryRepository.findByEntryTitle(entryTitle);
		
		if(toDelete.size() != 0) {
			for(TodoEntry x : toDelete) {
				todoEntryRepository.delete(x);
			}
			return toDelete;
		}
		else return null;
		
	}
	
	public TodoEntry deleteByOid(Long oid) {
		
		TodoEntry toDelete = todoEntryRepository.findByOid(oid);
		
		if(toDelete != null) {
			todoEntryRepository.delete(toDelete);
			return toDelete;
		}
		else return null;
	}
	
	public TodoEntry addEntry(TodoEntry entry) {
		return todoEntryRepository.save(entry);
	}
}
