package de.dhge.resapi.api;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.dhge.resapi.ResApiApplication;


public class ResApiRestController {

	@GetMapping(value = "/version", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> version() {
		return ResponseEntity.ok(ResApiApplication.version);
	}
	
	
	@GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> showList(){
		
		return //TODO 
	}
	
	@PostMapping(value = "/add", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> addEntry(@RequestParam(required=true) String entryTitle){
		
		return //TODO
	}
	
	@DeleteMapping(value = "/delete", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> deleteEntry(@RequestParam(required=false) String entryTitle){
		
		return //TOD
	}
	
	
}
