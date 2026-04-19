package com.daily.bread.favorites.exceptions;

public class VerseFavoriteDuplicateException extends RuntimeException {

	public VerseFavoriteDuplicateException() {
		super("Este versículo já está favoritado nesta data de leitura.");
	}
}
