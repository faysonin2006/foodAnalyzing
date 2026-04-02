from __future__ import annotations

import argparse
import math
import random
from pathlib import Path

import tensorflow as tf

from .model import (
    SemanticRecipeEncoder,
    build_training_pairs,
    load_corpus_jsonl,
    save_model_artifacts,
)


def main() -> None:
    parser = argparse.ArgumentParser(description="Train TensorFlow semantic reranker for recipes")
    parser.add_argument("--corpus", required=True, help="Path to exported recipe corpus JSONL")
    parser.add_argument("--model-dir", required=True, help="Output directory for trained model artifacts")
    parser.add_argument("--epochs", type=int, default=6)
    parser.add_argument("--batch-size", type=int, default=128)
    parser.add_argument("--learning-rate", type=float, default=1e-3)
    parser.add_argument("--max-pairs", type=int, default=900_000)
    args = parser.parse_args()

    recipes = load_corpus_jsonl(args.corpus)
    if not recipes:
        raise SystemExit("Corpus is empty. Export recipes first.")

    pairs = build_training_pairs(recipes)
    if not pairs:
        raise SystemExit("No training pairs were generated from the corpus.")
    if args.max_pairs > 0 and len(pairs) > args.max_pairs:
        pairs = random.Random(42).sample(pairs, args.max_pairs)

    queries = [pair[0] for pair in pairs]
    documents = [pair[1] for pair in pairs]

    model = SemanticRecipeEncoder()
    model.adapt_vocabulary(queries + documents)
    model.compile(optimizer=tf.keras.optimizers.Adam(learning_rate=args.learning_rate))

    dataset = tf.data.Dataset.from_tensor_slices((queries, documents))
    dataset = dataset.shuffle(len(pairs), reshuffle_each_iteration=True).batch(args.batch_size).prefetch(tf.data.AUTOTUNE)

    validation_size = max(1, math.floor(len(pairs) * 0.1))
    validation_dataset = dataset.take(max(1, math.ceil(validation_size / args.batch_size)))
    training_dataset = dataset.skip(max(1, math.ceil(validation_size / args.batch_size)))

    model.fit(training_dataset, validation_data=validation_dataset, epochs=args.epochs, verbose=1)

    save_model_artifacts(
        model,
        args.model_dir,
        extra_metadata={
            "recipes": len(recipes),
            "pairs": len(pairs),
            "epochs": args.epochs,
            "batch_size": args.batch_size,
            "max_pairs": args.max_pairs,
        },
    )
    print(f"Saved model artifacts to {Path(args.model_dir).resolve()}")


if __name__ == "__main__":
    main()
