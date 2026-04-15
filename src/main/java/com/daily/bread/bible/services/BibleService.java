package com.daily.bread.bible.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.daily.bread.bible.exceptions.BibleNotFoundException;
import com.daily.bread.bible.response.BibleBookResponse;
import com.daily.bread.bible.response.BibleChapterResponse;
import com.daily.bread.bible.response.BibleVerseCompareResponse;
import com.daily.bread.bible.response.BibleVerseDetailResponse;
import com.daily.bread.bible.response.BibleVerseResponse;
import com.daily.bread.bible.response.BibleVersionResponse;
import com.daily.bread.bible.response.BibleVersionVerseLineResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class BibleService {

	private static final List<BibleVersionResponse> VERSIONS = List.of(
			new BibleVersionResponse("nvi", "Nova Versão Internacional",
					"Texto NVI: direitos da Sociedade Bíblica Internacional. Dados agregados sob CC BY-NC (github.com/thiagobodruk/biblia). Uso não comercial."),
			new BibleVersionResponse("ara", "Almeida Revista e Atualizada",
					"Texto ARA: direitos reservados aos titulares da tradução. Fonte de dados: pacote biblias (github.com/damarals/biblias). Uso não comercial; verifique os termos junto à Sociedade Bíblica do Brasil."),
			new BibleVersionResponse("ntlh", "Nova Tradução na Linguagem de Hoje",
					"Texto NTLH: direitos da Sociedade Bíblica do Brasil (SBB). Fonte de dados: pacote biblias (github.com/damarals/biblias). Uso não comercial; confirme termos junto à SBB."));

	private static final Map<String, String> BIBLIA_RESOURCE = Map.of(
			"nvi", "bible/nvi.json",
			"ara", "bible/ara.json",
			"ntlh", "bible/ntlh.json");

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final Map<String, List<BibliaBookJson>> bibliaCache = new ConcurrentHashMap<>();

	public List<BibleVersionResponse> listVersions() {
		return VERSIONS;
	}

	public List<BibleBookResponse> listBooks(String versionId) {
		String v = normalizeVersion(versionId);
		List<BibliaBookJson> books = loadBiblia(v);
		List<BibleBookResponse> out = new ArrayList<>(books.size());
		for (int i = 0; i < books.size(); i++) {
			BibliaBookJson b = books.get(i);
			out.add(new BibleBookResponse(i + 1, b.abbrev, b.name));
		}
		return out;
	}

	public String requireVersion(String versionId) {
		return normalizeVersion(versionId);
	}

	public BibleVerseDetailResponse getVerse(String versionId, int bookNumber, int chapterNumber, int verseNumber) {
		if (verseNumber < 1) {
			throw new BibleNotFoundException("Versículo inválido.");
		}
		BibleChapterResponse chapter = getChapter(versionId, bookNumber, chapterNumber);
		for (BibleVerseResponse v : chapter.verses()) {
			if (v.verse() == verseNumber) {
				String vId = normalizeVersion(versionId);
				return new BibleVerseDetailResponse(vId, bookNumber, chapter.abbrev(), chapter.bookName(), chapterNumber,
						verseNumber, v.text());
			}
		}
		throw new BibleNotFoundException("Versículo não encontrado neste capítulo.");
	}

	public BibleVerseCompareResponse compareVerse(int bookNumber, int chapterNumber, int verseNumber) {
		if (bookNumber < 1 || bookNumber > 66 || chapterNumber < 1 || verseNumber < 1) {
			throw new BibleNotFoundException("Referência bíblica inválida.");
		}
		BibleChapterResponse refPt = getChapter("nvi", bookNumber, chapterNumber);
		List<BibleVersionVerseLineResponse> lines = new ArrayList<>(VERSIONS.size());
		for (BibleVersionResponse version : VERSIONS) {
			BibleVerseDetailResponse detail = getVerse(version.id(), bookNumber, chapterNumber, verseNumber);
			lines.add(new BibleVersionVerseLineResponse(version.id(), version.title(), detail.text()));
		}
		return new BibleVerseCompareResponse(bookNumber, refPt.abbrev(), refPt.bookName(), chapterNumber, verseNumber,
				List.copyOf(lines));
	}

	public BibleChapterResponse getChapter(String versionId, int bookNumber, int chapterNumber) {
		if (bookNumber < 1 || bookNumber > 66) {
			throw new BibleNotFoundException("Livro inválido: use um número entre 1 e 66.");
		}
		if (chapterNumber < 1) {
			throw new BibleNotFoundException("Capítulo inválido.");
		}
		String v = normalizeVersion(versionId);
		return getChapterBiblia(v, bookNumber, chapterNumber);
	}

	private BibleChapterResponse getChapterBiblia(String versionId, int bookNumber, int chapterNumber) {
		List<BibliaBookJson> books = loadBiblia(versionId);
		BibliaBookJson book = books.get(bookNumber - 1);
		if (chapterNumber > book.chapters.size()) {
			throw new BibleNotFoundException("Capítulo não encontrado neste livro.");
		}
		List<String> lines = book.chapters.get(chapterNumber - 1);
		List<BibleVerseResponse> verses = new ArrayList<>(lines.size());
		for (int i = 0; i < lines.size(); i++) {
			verses.add(new BibleVerseResponse(i + 1, lines.get(i)));
		}
		return new BibleChapterResponse(versionId, bookNumber, book.abbrev, book.name, chapterNumber, verses);
	}

	private List<BibliaBookJson> loadBiblia(String versionId) {
		return bibliaCache.computeIfAbsent(versionId, id -> {
			String path = BIBLIA_RESOURCE.get(id);
			if (path == null) {
				throw new BibleNotFoundException("Versão desconhecida.");
			}
			try {
				if ("ntlh".equals(id)) {
					return readBibliaBooksFlexible(path);
				}
				return readClasspathJsonArray(path, new TypeReference<List<BibliaBookJson>>() {
				});
			}
			catch (IOException e) {
				throw new IllegalStateException("Falha ao ler " + path, e);
			}
		});
	}

	/**
	 * NTLH (damarals/biblias) pode misturar versículo como string ou como array de linhas (poesia).
	 */
	private List<BibliaBookJson> readBibliaBooksFlexible(String classpathLocation) throws IOException {
		try (InputStream in = openStripBom(classpathLocation)) {
			JsonNode root = objectMapper.readTree(in);
			if (!root.isArray()) {
				throw new IOException("JSON da Bíblia deve ser um array de livros.");
			}
			List<BibliaBookJson> books = new ArrayList<>(root.size());
			for (JsonNode bookNode : root) {
				BibliaBookJson book = new BibliaBookJson();
				book.abbrev = textOrEmpty(bookNode.get("abbrev"));
				book.name = bookNode.hasNonNull("name") ? textOrEmpty(bookNode.get("name")) : book.abbrev;
				List<List<String>> chapters = new ArrayList<>();
				JsonNode chaptersNode = bookNode.get("chapters");
				if (chaptersNode != null && chaptersNode.isArray()) {
					for (JsonNode chapterNode : chaptersNode) {
						List<String> verses = new ArrayList<>();
						if (chapterNode.isArray()) {
							for (JsonNode verseNode : chapterNode) {
								verses.add(flattenVerseNode(verseNode));
							}
						}
						chapters.add(verses);
					}
				}
				book.chapters = chapters;
				books.add(book);
			}
			return books;
		}
	}

	private static String textOrEmpty(JsonNode n) {
		return n == null || n.isNull() ? "" : n.asText("");
	}

	private static String flattenVerseNode(JsonNode verseNode) {
		if (verseNode == null || verseNode.isNull()) {
			return "";
		}
		if (verseNode.isTextual()) {
			return verseNode.asText();
		}
		if (verseNode.isArray()) {
			StringBuilder sb = new StringBuilder();
			for (JsonNode part : verseNode) {
				if (sb.length() > 0) {
					sb.append(' ');
				}
				if (part.isTextual()) {
					sb.append(part.asText());
				}
				else if (part.isArray()) {
					sb.append(flattenVerseNode(part));
				}
			}
			return sb.toString();
		}
		return verseNode.asText("");
	}

	private String normalizeVersion(String versionId) {
		if (versionId == null || versionId.isBlank()) {
			throw new BibleNotFoundException("Versão desconhecida.");
		}
		String v = versionId.trim().toLowerCase(Locale.ROOT);
		if (!"ntlh".equals(v) && !"nvi".equals(v) && !"ara".equals(v)) {
			throw new BibleNotFoundException("Versão desconhecida. Use: nvi, ara ou ntlh.");
		}
		return v;
	}

	private <T> T readClasspathJson(String classpathLocation, TypeReference<T> type) throws IOException {
		try (InputStream in = openStripBom(classpathLocation)) {
			return objectMapper.readValue(in, type);
		}
	}

	private <T> T readClasspathJsonArray(String classpathLocation, TypeReference<T> type) throws IOException {
		return readClasspathJson(classpathLocation, type);
	}

	private InputStream openStripBom(String classpathLocation) throws IOException {
		ClassPathResource resource = new ClassPathResource(classpathLocation);
		InputStream raw = resource.getInputStream();
		PushbackInputStream in = new PushbackInputStream(raw, 3);
		byte[] bom = new byte[3];
		int read = in.read(bom);
		if (read == 3 && (bom[0] & 0xFF) == 0xEF && (bom[1] & 0xFF) == 0xBB && (bom[2] & 0xFF) == 0xBF) {
			return in;
		}
		if (read > 0) {
			in.unread(bom, 0, read);
		}
		return in;
	}
}
