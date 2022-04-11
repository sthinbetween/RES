package de.dhge.resapi.model;

import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "entries")
public class TodoEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private Long oid;
	private LocalDateTime creationDate;
	private String entryTitle;
	
	@Override
	public int hashCode() {
		return Objects.hash(creationDate, entryTitle, oid);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TodoEntry other = (TodoEntry) obj;
		return Objects.equals(creationDate, other.creationDate) && Objects.equals(entryTitle, other.entryTitle)
				&& Objects.equals(oid, other.oid);
	}
	
	public String toJSONString() {
		StringBuilder strBuilder = new StringBuilder();
		
		strBuilder.append("{\"oid\": \"" + oid + "\",");
		strBuilder.append("\"title\": \"" + entryTitle + "\",");
		strBuilder.append("\"date\": \"" + creationDate + "\"}");
		
		return strBuilder.toString();
	}

	public Long getOid() {
		return oid;
	}

	public LocalDateTime getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(LocalDateTime creationDate) {
		this.creationDate = creationDate;
	}

	public String getEntryTitle() {
		return entryTitle;
	}

	public void setEntryTitle(String entryTitle) {
		this.entryTitle = entryTitle;
	}
	
}
