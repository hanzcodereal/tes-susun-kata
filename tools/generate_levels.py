#!/usr/bin/env python3
"""Pembuat level Susun Kata (Hanz Dev Official).

Membaca tools/kata.txt, membuat teka-teki silang kecil per level, memverifikasi
setiap level (kata valid, huruf cukup, grid tersambung, tidak ada kata liar),
lalu menulis app/src/main/assets/levels.json. Deterministik (seed tetap).
Pakai:  python3 tools/generate_levels.py
"""
import json, random, sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DICT = sorted(set(Path(__file__).with_name("kata.txt").read_text().split()))
DSET = set(DICT)
MAX_COLS, MAX_ROWS = 7, 8
SEED = 20261113


def formable(word, pool):
    need, have = Counter(word), Counter(pool)
    return all(have[ch] >= n for ch, n in need.items())


def try_layout(words, rng):
    """Susun kata silang. Kembalikan (placements, crossings) atau None."""
    grid, dirs, placed, cross_total = {}, {}, [], 0
    first = words[0]
    for i, ch in enumerate(first):
        grid[(0, i)] = ch
        dirs[(0, i)] = {0}
    placed.append((first, 0, 0, 0))
    for w in words[1:]:
        best = []
        for d in (0, 1):
            dr, dc = (0, 1) if d == 0 else (1, 0)
            pr, pc = dc, dr  # arah tegak lurus
            for (gr, gc), gch in list(grid.items()):
                for i, ch in enumerate(w):
                    if ch != gch:
                        continue
                    r0, c0 = gr - i * dr, gc - i * dc
                    ok, cross = True, 0
                    if (r0 - dr, c0 - dc) in grid or (r0 + len(w) * dr, c0 + len(w) * dc) in grid:
                        continue
                    for k, wc in enumerate(w):
                        pos = (r0 + k * dr, c0 + k * dc)
                        if pos in grid:
                            if grid[pos] != wc or d in dirs[pos]:
                                ok = False
                                break
                            cross += 1
                        else:
                            if (pos[0] + pr, pos[1] + pc) in grid or (pos[0] - pr, pos[1] - pc) in grid:
                                ok = False
                                break
                    if not ok or cross == 0:
                        continue
                    cells = list(grid.keys()) + [(r0 + k * dr, c0 + k * dc) for k in range(len(w))]
                    rs = [p[0] for p in cells]
                    cs = [p[1] for p in cells]
                    h, wd = max(rs) - min(rs) + 1, max(cs) - min(cs) + 1
                    if h > MAX_ROWS or wd > MAX_COLS:
                        continue
                    score = cross * 10 - h * wd - abs(h - wd) * 0.5 + rng.random() * 3
                    best.append((score, r0, c0, d, cross))
        if not best:
            return None
        best.sort(reverse=True)
        _, r0, c0, d, cross = best[0]
        dr, dc = (0, 1) if d == 0 else (1, 0)
        for k, wc in enumerate(w):
            pos = (r0 + k * dr, c0 + k * dc)
            grid[pos] = wc
            dirs.setdefault(pos, set()).add(d)
        placed.append((w, r0, c0, d))
        cross_total += cross
    return placed, cross_total


def build(words, rng, tries=400):
    best = None
    for _ in range(tries):
        rest = words[1:]
        rng.shuffle(rest)
        order = [words[0]] + sorted(rest, key=lambda x: -len(x) + rng.random())
        res = try_layout(order, rng)
        if not res:
            continue
        placed, cross = res
        rs = [p[1] for p in placed] + [p[1] + (len(p[0]) - 1) * p[3] for p in placed]
        cs = [p[2] for p in placed] + [p[2] + (len(p[0]) - 1) * (1 - p[3]) for p in placed]
        area = (max(rs) - min(rs) + 1) * (max(cs) - min(cs) + 1)
        key = (cross, -area)
        if best is None or key > best[0]:
            best = (key, placed)
    return best[1] if best else None


def normalize(placed):
    r_min = min(min(p[1], p[1] + (len(p[0]) - 1) * p[3]) for p in placed)
    c_min = min(min(p[2], p[2] + (len(p[0]) - 1) * (1 - p[3])) for p in placed)
    out = [[w, r - r_min, c - c_min, d] for w, r, c, d in placed]
    rows = max(r + (len(w) - 1) * d for w, r, c, d in out) + 1
    cols = max(c + (len(w) - 1) * (1 - d) for w, r, c, d in out) + 1
    return out, rows, cols


def verify(level):
    """Pemeriksa: kata valid, huruf cukup, tersambung, tidak ada kata liar."""
    pool = level["letters"]
    grid = {}
    for w, r, c, d in level["words"]:
        assert w in DSET and formable(w, pool), f"kata tidak valid: {w}"
        for k, ch in enumerate(w):
            pos = (r + k * d, c + k * (1 - d))
            assert grid.get(pos, ch) == ch, f"tabrakan huruf di {pos}"
            grid[pos] = ch
    assert max(p[0] for p in grid) + 1 == level["rows"] and max(p[1] for p in grid) + 1 == level["cols"]
    # tersambung
    seen, stack = set(), [next(iter(grid))]
    while stack:
        p = stack.pop()
        if p in seen:
            continue
        seen.add(p)
        for dr, dc in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (p[0] + dr, p[1] + dc)
            if q in grid and q not in seen:
                stack.append(q)
    assert len(seen) == len(grid), "grid tidak tersambung"
    # tidak ada kata liar: setiap run >=2 harus kata yang terdaftar
    listed = {(w, r, c, d) for w, r, c, d in level["words"]}
    for d in (0, 1):
        for (r, c) in grid:
            prev = (r - d, c - (1 - d))
            if prev in grid:
                continue
            run, k = "", 0
            while (r + k * d, c + k * (1 - d)) in grid:
                run += grid[(r + k * d, c + k * (1 - d))]
                k += 1
            if len(run) >= 2:
                assert (run, r, c, d) in listed, f"kata liar {run} di {(r, c)}"
    for b in level["bonus"]:
        assert b in DSET and formable(b, pool)
    assert not set(b for b in level["bonus"]) & set(w for w, *_ in level["words"])


def main():
    rng = random.Random(SEED)
    seen_sets, bases = set(), []
    for w in DICT:
        key = "".join(sorted(w))
        if 4 <= len(w) <= 7 and key not in seen_sets:
            subs = [x for x in DICT if len(x) >= 3 and formable(x, w)]
            if len(subs) >= (3 if len(w) == 4 else 4):
                seen_sets.add(key)
                bases.append((w, subs))
    first = [b for b in bases if b[0] == "LAUT"]
    others = [b for b in bases if b[0] != "LAUT"]
    rng.shuffle(others)
    others.sort(key=lambda b: (len(b[0]) + rng.random() * 1.2))
    bases = first + others
    levels = []
    for idx, (base, subs) in enumerate(bases, start=1):
        n_t = max(2, min(8, 2 + (idx - 1) // 4))
        n_t = min(n_t, len(subs))
        for attempt in range(40):
            lrng = random.Random(SEED + idx * 101 + attempt)
            cand = [x for x in subs if x != base]
            lrng.shuffle(cand)
            cand.sort(key=lambda x: -len(x) + lrng.random() * 2.5)
            chosen = [base] + cand[: max(0, n_t - 1)]
            placed = build(chosen, lrng)
            if not placed:
                continue
            words, rows, cols = normalize(placed)
            if len(words) < min(n_t, 2):
                continue
            in_grid = {w for w, *_ in words}
            lvl = {
                "n": len(levels) + 1,
                "letters": base,
                "rows": rows,
                "cols": cols,
                "words": words,
                "bonus": sorted(x for x in subs if x not in in_grid),
            }
            try:
                verify(lvl)
            except AssertionError as e:
                print("gagal verifikasi:", base, e, file=sys.stderr)
                continue
            levels.append(lvl)
            break
        else:
            print("lewati", base, file=sys.stderr)
    out = ROOT / "app/src/main/assets/levels.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps({"version": 1, "levels": levels}, ensure_ascii=False, separators=(",", ":")))
    sizes = Counter(len(l["words"]) for l in levels)
    print(f"{len(levels)} level ditulis ke {out}  (jumlah kata per level: {dict(sorted(sizes.items()))})")
    for l in levels[:6]:
        print(l["n"], l["letters"], [w[0] for w in l["words"]], f'{l["rows"]}x{l["cols"]}', "bonus", len(l["bonus"]))


if __name__ == "__main__":
    main()
