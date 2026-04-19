package com.daily.bread.favorites.exceptions;

public class VerseFavoriteNotFoundException extends RuntimeException {

	public VerseFavoriteNotFoundException() {
		super("Favorito não encontrado.");
	}
}
