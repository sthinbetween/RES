package de.dhge.resapi.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.dhge.resapi.ResApiApplication;
import de.dhge.resapi.logic.ContentManager;

@RestController
public class ResApiRestController {

	ContentManager contentManager;
	
	@Autowired
	public ResApiRestController(ContentManager contentManager) {
		this.contentManager = contentManager;
	}
	
	@GetMapping(value = "/version", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> version(@RequestParam(required=true) String secret) {
		
		if(secret != null && secret.equals(ResApiApplication.secret)) {
			return ResponseEntity.ok(ResApiApplication.version);
		}
		
		else return ResponseEntity.badRequest().body("secret is false");
		
	}
		
	@GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> showList(@RequestParam(required=true) String secret){
		
		if(secret != null && secret.equals(ResApiApplication.secret)) {
			return contentManager.showEntries();	
		}
		else return ResponseEntity.badRequest().body("secret is false");
	}
	
	@PostMapping(value = "/add", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> addEntry(@RequestParam(required=true) String secret, @RequestParam(required=true) String entryTitle){
		
		if(secret != null && secret.equals(ResApiApplication.secret)) {
			if(entryTitle != null && entryTitle.length() != 0) {
				return contentManager.addEntry(entryTitle);
			}
			else return ResponseEntity.badRequest().body("no data was given");
		}
		else return ResponseEntity.badRequest().body("secret is false");
	}
	
	@DeleteMapping(value = "/delete", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> deleteEntry(@RequestParam(required=true) String secret, @RequestParam(required=false) String entryTitle, @RequestParam(required = false) Long entryOid){
		
		if(secret != null && secret.equals(ResApiApplication.secret)) {
			if(entryTitle != null && entryTitle.length() != 0) {
				return contentManager.deleteEntryTitle(entryTitle);
			}
			if(entryOid != null) {
				return contentManager.deleteEntryOid(entryOid);
			}
			else return ResponseEntity.badRequest().body("no data was given");
		}
		else return ResponseEntity.badRequest().body("secret is false");
	}
}
