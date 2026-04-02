from __future__ import annotations

import json
import random
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Sequence

import tensorflow as tf

DEFAULT_MAX_TOKENS = 20_000
DEFAULT_SEQUENCE_LENGTH = 48
DEFAULT_EMBEDDING_DIM = 128
DEFAULT_PROJECTION_DIM = 128
DEFAULT_TEMPERATURE = 0.07


@dataclass(frozen=True)
class RecipeDocument:
    recipe_id: int
    title: str
    category: str
    ingredients: list[str]


def _coerce_string_list(raw: object) -> list[str]:
    if raw is None:
        return []
    if isinstance(raw, str):
        return [raw]
    if isinstance(raw, Sequence):
        values: list[str] = []
        for item in raw:
            text = str(item or "").strip()
            if text:
                values.append(text)
        return values
    return []


def normalize_text(text: str | None) -> str:
    if not text:
        return ""
    text = text.lower()
    text = re.sub(r"[^\w\s]+", " ", text, flags=re.UNICODE)
    text = re.sub(r"\s+", " ", text, flags=re.UNICODE)
    return text.strip()


def build_document_text(recipe: RecipeDocument | dict) -> str:
    if isinstance(recipe, dict):
        primary_text = str(recipe.get("primaryText") or recipe.get("title") or "")
        secondary_text = str(recipe.get("secondaryText") or recipe.get("secondary_text") or "")
        category_text = str(recipe.get("category") or "")
        brand_text = str(recipe.get("brand") or "")
        search_terms = _coerce_string_list(recipe.get("searchTerms") or recipe.get("search_terms"))
        ingredients = _coerce_string_list(recipe.get("ingredients"))
        if primary_text or secondary_text or brand_text or search_terms:
            parts: list[str] = []
            if primary_text:
                parts.extend([primary_text, primary_text])
            if category_text:
                parts.append(category_text)
            if brand_text:
                parts.append(brand_text)
            if secondary_text:
                parts.append(secondary_text)
            if search_terms:
                parts.append(" ".join(search_terms))
                parts.extend(search_terms[:4])
            if ingredients:
                parts.append(" ".join(ingredients))
                parts.extend(ingredients[:4])
            return normalize_text(" ".join(parts))
        recipe = RecipeDocument(
            recipe_id=int(recipe.get("recipe_id") or recipe.get("recipeId") or 0),
            title=str(recipe.get("title") or ""),
            category=category_text,
            ingredients=ingredients,
        )
    parts: list[str] = []
    if recipe.title:
        parts.extend([recipe.title, recipe.title])
    if recipe.category:
        parts.append(recipe.category)
    if recipe.ingredients:
        parts.append(" ".join(recipe.ingredients))
        parts.extend(recipe.ingredients[:4])
    return normalize_text(" ".join(parts))


def build_query_variants(recipe: RecipeDocument) -> list[str]:
    title = normalize_text(recipe.title)
    category = normalize_text(recipe.category)
    ingredients = [normalize_text(item) for item in recipe.ingredients if normalize_text(item)]

    variants: list[str] = []
    if title:
        variants.append(title)
    if title and category:
        variants.append(f"{title} {category}")
    if title and ingredients:
        variants.append(f"{title} {ingredients[0]}")
    if title:
        variants.extend(_build_title_phrases(title))
    if len(ingredients) >= 2:
        variants.append(" ".join(ingredients[:2]))
    if category and ingredients:
        variants.append(f"{category} {ingredients[0]}")
    if ingredients:
        variants.append(ingredients[0])
    if len(ingredients) >= 3:
        variants.append(" ".join(ingredients[:3]))
    variants.extend(ingredients[:3])

    if title:
        variants.extend(_build_prefix_variants(title))
        typo = _build_typo_variant(title)
        if typo:
            variants.append(typo)
    for ingredient in ingredients[:2]:
        variants.extend(_build_prefix_variants(ingredient))
        typo = _build_typo_variant(ingredient)
        if typo:
            variants.append(typo)

    deduped: list[str] = []
    seen: set[str] = set()
    for variant in variants:
        normalized = normalize_text(variant)
        if normalized and normalized not in seen:
            seen.add(normalized)
            deduped.append(normalized)
    return deduped


def _meaningful_tokens(text: str, *, min_len: int = 3) -> list[str]:
    return [token for token in normalize_text(text).split() if len(token) >= min_len]


def _build_title_phrases(title: str) -> list[str]:
    tokens = _meaningful_tokens(title)
    if not tokens:
        return []
    phrases: list[str] = []
    if len(tokens) >= 2:
        phrases.append(" ".join(tokens[:2]))
    if len(tokens) >= 3:
        phrases.append(" ".join(tokens[:3]))
    for token in tokens[:3]:
        phrases.append(token)
    return phrases


def _build_prefix_variants(text: str) -> list[str]:
    normalized = normalize_text(text)
    if not normalized:
        return []
    tokens = _meaningful_tokens(normalized)
    if not tokens:
        return []

    variants: list[str] = []
    first = tokens[0]
    prefix_lengths = [3, min(5, len(first)), min(8, len(first))]
    for size in prefix_lengths:
        if 2 < size < len(first):
            variants.append(first[:size])

    if len(tokens) >= 2:
        combined = f"{tokens[0]} {tokens[1]}"
        if len(combined) > 6:
            variants.append(combined[: min(len(combined), max(len(tokens[0]) + 2, 6))].strip())
        variants.append(combined)

    return variants


def _build_typo_variant(text: str) -> str | None:
    normalized = normalize_text(text)
    if len(normalized) < 6:
        return None
    middle = len(normalized) // 2
    if normalized[middle] == " ":
        middle = min(len(normalized) - 2, middle + 1)
    variant = (normalized[:middle] + normalized[middle + 1 :]).strip()
    return variant if variant and variant != normalized else None


class SemanticRecipeEncoder(tf.keras.Model):
    def __init__(
        self,
        max_tokens: int = DEFAULT_MAX_TOKENS,
        sequence_length: int = DEFAULT_SEQUENCE_LENGTH,
        embedding_dim: int = DEFAULT_EMBEDDING_DIM,
        projection_dim: int = DEFAULT_PROJECTION_DIM,
        temperature: float = DEFAULT_TEMPERATURE,
    ) -> None:
        super().__init__()
        self.max_tokens = max_tokens
        self.sequence_length = sequence_length
        self.embedding_dim = embedding_dim
        self.projection_dim = projection_dim
        self.temperature = temperature

        self.vectorizer = tf.keras.layers.TextVectorization(
            max_tokens=max_tokens,
            output_mode="int",
            output_sequence_length=sequence_length,
            standardize="lower_and_strip_punctuation",
        )
        self.embedding = tf.keras.layers.Embedding(max_tokens + 2, embedding_dim, mask_zero=True)
        self.dropout = tf.keras.layers.Dropout(0.15)
        self.projection = tf.keras.Sequential(
            [
                tf.keras.layers.Dense(projection_dim * 2, activation="relu"),
                tf.keras.layers.Dropout(0.15),
                tf.keras.layers.Dense(projection_dim),
            ]
        )
        self.loss_tracker = tf.keras.metrics.Mean(name="loss")

    @property
    def metrics(self) -> list[tf.keras.metrics.Metric]:
        return [self.loss_tracker]

    def adapt_vocabulary(self, texts: Sequence[str]) -> None:
        dataset = tf.data.Dataset.from_tensor_slices(list(texts)).batch(256)
        self.vectorizer.adapt(dataset)

    def encode_text(self, texts: Sequence[str] | tf.Tensor, training: bool = False) -> tf.Tensor:
        if not isinstance(texts, tf.Tensor):
            texts = tf.convert_to_tensor(list(texts), dtype=tf.string)
        tokens = self.vectorizer(texts)
        embeddings = self.embedding(tokens)
        mask = tf.cast(tf.not_equal(tokens, 0), tf.float32)[..., tf.newaxis]
        masked = embeddings * mask
        pooled = tf.math.divide_no_nan(tf.reduce_sum(masked, axis=1), tf.reduce_sum(mask, axis=1))
        projected = self.projection(self.dropout(pooled, training=training), training=training)
        return tf.math.l2_normalize(projected, axis=-1)

    def call(self, inputs: tuple[tf.Tensor, tf.Tensor], training: bool = False) -> tf.Tensor:
        queries, documents = inputs
        query_embeddings = self.encode_text(queries, training=training)
        doc_embeddings = self.encode_text(documents, training=training)
        return tf.matmul(query_embeddings, doc_embeddings, transpose_b=True) / self.temperature

    def train_step(self, data):
        queries, documents = data
        labels = tf.range(tf.shape(queries)[0])
        with tf.GradientTape() as tape:
            logits = self((queries, documents), training=True)
            loss_q = tf.reduce_mean(
                tf.keras.losses.sparse_categorical_crossentropy(labels, logits, from_logits=True)
            )
            loss_d = tf.reduce_mean(
                tf.keras.losses.sparse_categorical_crossentropy(labels, tf.transpose(logits), from_logits=True)
            )
            loss = (loss_q + loss_d) / 2.0
            if self.losses:
                loss += tf.add_n(self.losses)

        gradients = tape.gradient(loss, self.trainable_variables)
        self.optimizer.apply_gradients(zip(gradients, self.trainable_variables))
        self.loss_tracker.update_state(loss)
        return {metric.name: metric.result() for metric in self.metrics}

    def test_step(self, data):
        queries, documents = data
        labels = tf.range(tf.shape(queries)[0])
        logits = self((queries, documents), training=False)
        loss_q = tf.reduce_mean(
            tf.keras.losses.sparse_categorical_crossentropy(labels, logits, from_logits=True)
        )
        loss_d = tf.reduce_mean(
            tf.keras.losses.sparse_categorical_crossentropy(labels, tf.transpose(logits), from_logits=True)
        )
        loss = (loss_q + loss_d) / 2.0
        self.loss_tracker.update_state(loss)
        return {metric.name: metric.result() for metric in self.metrics}


def build_training_pairs(recipes: Sequence[RecipeDocument]) -> list[tuple[str, str]]:
    pairs: list[tuple[str, str]] = []
    for recipe in recipes:
        document = build_document_text(recipe)
        if not document:
            continue
        for query in build_query_variants(recipe):
            pairs.append((query, document))
    random.Random(42).shuffle(pairs)
    return pairs


def save_model_artifacts(model: SemanticRecipeEncoder, model_dir: str | Path, extra_metadata: dict | None = None) -> None:
    model_path = Path(model_dir)
    model_path.mkdir(parents=True, exist_ok=True)

    vocabulary = model.vectorizer.get_vocabulary()
    (model_path / "vocabulary.json").write_text(json.dumps(vocabulary, ensure_ascii=False, indent=2), encoding="utf-8")
    config = {
        "max_tokens": model.max_tokens,
        "sequence_length": model.sequence_length,
        "embedding_dim": model.embedding_dim,
        "projection_dim": model.projection_dim,
        "temperature": model.temperature,
    }
    (model_path / "config.json").write_text(json.dumps(config, ensure_ascii=False, indent=2), encoding="utf-8")
    if extra_metadata is not None:
        (model_path / "metadata.json").write_text(json.dumps(extra_metadata, ensure_ascii=False, indent=2), encoding="utf-8")

    dummy = tf.constant(["warmup query"], dtype=tf.string)
    model((dummy, dummy), training=False)
    model.save_weights(model_path / "weights.weights.h5")


def load_model_artifacts(model_dir: str | Path) -> SemanticRecipeEncoder:
    model_path = Path(model_dir)
    config = json.loads((model_path / "config.json").read_text(encoding="utf-8"))
    vocabulary = json.loads((model_path / "vocabulary.json").read_text(encoding="utf-8"))

    model = SemanticRecipeEncoder(**config)
    model.vectorizer.set_vocabulary(vocabulary)
    dummy = tf.constant(["warmup query"], dtype=tf.string)
    model((dummy, dummy), training=False)
    model.load_weights(model_path / "weights.weights.h5")
    return model


def load_corpus_jsonl(corpus_path: str | Path) -> list[RecipeDocument]:
    path = Path(corpus_path)
    documents: list[RecipeDocument] = []
    with path.open("r", encoding="utf-8") as handle:
        for line in handle:
            line = line.strip()
            if not line:
                continue
            item = json.loads(line)
            documents.append(
                RecipeDocument(
                    recipe_id=int(item.get("recipe_id") or item.get("recipeId") or 0),
                    title=str(item.get("title") or ""),
                    category=str(item.get("category") or ""),
                    ingredients=[str(value) for value in item.get("ingredients") or []],
                )
            )
    return documents


def rerank_candidates(model: SemanticRecipeEncoder, query: str, candidates: Sequence[dict]) -> list[dict]:
    ranked = rank_text_candidates(model, query, candidates, id_keys=("recipeId", "recipe_id"))
    return [
        {"recipeId": int(item["id"]), "score": item["score"]}
        for item in ranked
        if str(item.get("id") or "").strip()
    ]


def rank_text_candidates(
    model: SemanticRecipeEncoder,
    query: str,
    candidates: Sequence[dict],
    *,
    id_keys: Sequence[str] = ("id",),
) -> list[dict]:
    query_text = normalize_text(query)
    if not query_text:
        return []

    docs = [build_document_text(candidate) for candidate in candidates]
    query_embedding = model.encode_text([query_text], training=False)
    doc_embeddings = model.encode_text(docs, training=False)
    scores = tf.squeeze(tf.matmul(query_embedding, doc_embeddings, transpose_b=True), axis=0).numpy().tolist()

    ranked: list[dict] = []
    for candidate, score in zip(candidates, scores):
        candidate_id = None
        for key in id_keys:
            candidate_id = candidate.get(key)
            if candidate_id is not None and str(candidate_id).strip():
                break
        if candidate_id is None:
            continue
        ranked.append({"id": str(candidate_id), "score": float(score)})
    ranked.sort(key=lambda item: item["score"], reverse=True)
    return ranked
