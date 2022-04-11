package de.dhge.resapi.logic;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import de.dhge.resapi.model.TodoEntry;
import de.dhge.resapi.service.TodoEntryRepository;
import de.dhge.resapi.service.impl.TodoEntryService;

@Service
public class ContentManager {
	
	TodoEntryService todoEntryService;
	
	@Autowired
	public ContentManager(TodoEntryService todoEntryService) {
		this.todoEntryService = todoEntryService;
	}
	
	
	public ResponseEntity<?> addEntry(String title) {
		
		TodoEntry toAdd = new TodoEntry();
		
		toAdd.setEntryTitle(title);
		toAdd.setCreationDate(LocalDateTime.now());
		
		TodoEntry added = todoEntryService.addEntry(toAdd);
		
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("{\"added Entry\": [");
		strBuilder.append(added.toJSONString());
		strBuilder.append("]}");
		
		return ResponseEntity.ok(strBuilder.toString());	
	}
	
	public ResponseEntity<?> deleteEntryTitle(String title) {
		
		ArrayList<TodoEntry> deleted = todoEntryService.deleteByTitle(title);
		if(deleted != null) {
			
			StringBuilder strBuilder = new StringBuilder();
			strBuilder.append("{\"deleted Entries\": [");
			
			for(TodoEntry x : deleted) {
				strBuilder.append(x.toJSONString());
				strBuilder.append(",");
			}
			
			strBuilder.deleteCharAt(strBuilder.length() - 1);
			strBuilder.append("]}");
			
			return ResponseEntity.ok(strBuilder.toString());
		}
		else {
			return ResponseEntity.badRequest().body("no such Entry");
		}
		
	}
	
	public ResponseEntity<?> deleteEntryOid(Long oid){
		
		TodoEntry deleted = todoEntryService.deleteByOid(oid);
		if(deleted != null) {
			
			StringBuilder strBuilder = new StringBuilder();
			strBuilder.append("{\"deleted Entry\": [");
			strBuilder.append(deleted.toJSONString());
			strBuilder.append("]}");
			
			return ResponseEntity.ok(strBuilder.toString());
		}
		else {
			return ResponseEntity.badRequest().body("no such Entry");
		}
		
	}
	
	public ResponseEntity<?> showEntries(){
		
		ArrayList<TodoEntry> allEntries = todoEntryService.findAll();
		if(allEntries != null) {
			
			StringBuilder strBuilder = new StringBuilder();
			strBuilder.append("{\"all Entries\": [");
			
			for(TodoEntry x : allEntries) {
				strBuilder.append(x.toJSONString());
				strBuilder.append(",");
			}
			
			strBuilder.deleteCharAt(strBuilder.length() - 1);
			strBuilder.append("]}");
			
			return ResponseEntity.ok(strBuilder.toString());
		}
		else {
			return ResponseEntity.badRequest().body("no Entries");
		}
		
		
	}

}
