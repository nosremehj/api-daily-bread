package com.daily.bread.bible.services;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
class BibliaBookJson {

	public String abbrev;
	public String name;
	public List<List<String>> chapters;
}
