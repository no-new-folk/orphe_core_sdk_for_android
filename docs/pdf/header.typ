// ============================================================
// README.pdf 用のTypstスタイル拡張
// pandoc --include-in-header で読み込む
// このファイルは pandoc が生成した highlighting-definitions の
// 後に評価されるため、Skylighting 関連の関数を上書きできる。
// ============================================================

// --- カラーパレット ---
#let code-bg = luma(245)
#let code-inline-bg = luma(240)
#let code-accent = rgb("#0a84ff")
#let code-border = luma(220)
#let code-font = ("Menlo", "Hiragino Sans")

// --- 段落・行間 ---
#set par(leading: 0.75em, spacing: 0.85em)

// --- インラインコード（markdown の `foo`）にチップ風の背景を付与 ---
// 注意: pandoc の *Tok 関数を後で上書きするため、シンタックスハイライト
// のトークンはこのルールに引っかからない。
#show raw.where(block: false): it => box(
  fill: code-inline-bg,
  outset: (y: 2pt),
  inset: (x: 3pt),
  radius: 3pt,
)[#text(font: code-font, size: 0.9em, fill: rgb("#24292f"))[#it.text]]

// --- ハイライトなしのコードブロック（`raw` ブロック） ---
#show raw.where(block: true): it => block(
  fill: code-bg,
  stroke: (left: 3pt + code-accent, rest: 0.5pt + code-border),
  inset: (x: 12pt, y: 10pt),
  radius: 4pt,
  width: 100%,
  above: 1em,
  below: 1em,
  breakable: true,
)[
  #set text(font: code-font, size: 9pt)
  #set par(leading: 0.55em, justify: false)
  #it
]

// --- 見出しの階層感を強調 ---
#show heading.where(level: 1): it => block(
  above: 1.6em, below: 0.7em,
)[#text(size: 1.6em, weight: "bold")[#it.body]]

#show heading.where(level: 2): it => block(
  above: 1.3em, below: 0.6em,
)[#text(size: 1.25em, weight: "bold")[#it.body]]

#show heading.where(level: 3): it => block(
  above: 1.1em, below: 0.5em,
)[#text(size: 1.1em, weight: "bold")[#it.body]]

// --- リスト間隔 ---
#set list(spacing: 0.6em, indent: 1em)
#set enum(spacing: 0.6em, indent: 1em)

// --- リンク色 ---
#show link: set text(fill: code-accent)

// ============================================================
// シンタックスハイライト（Skylighting）の見た目を上書きする。
// pandoc が生成した *Tok 関数は `text(raw(s))` を返すため、
// 上の raw.where(block: false) ルールに引っかかり、各トークンが
// チップ状にレイアウトされてしまう。ここで `raw(s)` を外し、
// 純粋な `text(...)` に置き換えることで問題を解消する。
// ============================================================

#let __mono(s) = text(font: code-font, size: 9pt, s)
#let AlertTok(s) = text(fill: rgb("#ef2929"), __mono(s))
#let AnnotationTok(s) = text(weight: "bold", style: "italic", fill: rgb("#8f5902"), __mono(s))
#let AttributeTok(s) = text(fill: rgb("#204a87"), __mono(s))
#let BaseNTok(s) = text(fill: rgb("#0000cf"), __mono(s))
#let BuiltInTok(s) = __mono(s)
#let CharTok(s) = text(fill: rgb("#4e9a06"), __mono(s))
#let CommentTok(s) = text(style: "italic", fill: rgb("#8f5902"), __mono(s))
#let CommentVarTok(s) = text(weight: "bold", style: "italic", fill: rgb("#8f5902"), __mono(s))
#let ConstantTok(s) = text(fill: rgb("#8f5902"), __mono(s))
#let ControlFlowTok(s) = text(weight: "bold", fill: rgb("#204a87"), __mono(s))
#let DataTypeTok(s) = text(fill: rgb("#204a87"), __mono(s))
#let DecValTok(s) = text(fill: rgb("#0000cf"), __mono(s))
#let DocumentationTok(s) = text(weight: "bold", style: "italic", fill: rgb("#8f5902"), __mono(s))
#let ErrorTok(s) = text(weight: "bold", fill: rgb("#a40000"), __mono(s))
#let ExtensionTok(s) = __mono(s)
#let FloatTok(s) = text(fill: rgb("#0000cf"), __mono(s))
#let FunctionTok(s) = text(weight: "bold", fill: rgb("#204a87"), __mono(s))
#let ImportTok(s) = __mono(s)
#let InformationTok(s) = text(weight: "bold", style: "italic", fill: rgb("#8f5902"), __mono(s))
#let KeywordTok(s) = text(weight: "bold", fill: rgb("#204a87"), __mono(s))
#let NormalTok(s) = __mono(s)
#let OperatorTok(s) = text(weight: "bold", fill: rgb("#ce5c00"), __mono(s))
#let OtherTok(s) = text(fill: rgb("#8f5902"), __mono(s))
#let PreprocessorTok(s) = text(style: "italic", fill: rgb("#8f5902"), __mono(s))
#let RegionMarkerTok(s) = __mono(s)
#let SpecialCharTok(s) = text(weight: "bold", fill: rgb("#ce5c00"), __mono(s))
#let SpecialStringTok(s) = text(fill: rgb("#4e9a06"), __mono(s))
#let StringTok(s) = text(fill: rgb("#4e9a06"), __mono(s))
#let VariableTok(s) = text(fill: rgb("#000000"), __mono(s))
#let VerbatimStringTok(s) = text(fill: rgb("#4e9a06"), __mono(s))
#let WarningTok(s) = text(weight: "bold", style: "italic", fill: rgb("#8f5902"), __mono(s))

// Skylighting 関数を上書き:
//   * 背景 → 独自の code-bg
//   * 左に code-accent のバー
//   * padding / radius / breakable を追加
//   * EndLine を Typst の linebreak() に差し替え
#let EndLine() = linebreak()
#let Skylighting(fill: none, number: false, start: 1, sourcelines) = {
  let blocks = []
  let lnum = start - 1
  for ln in sourcelines {
    if number {
      lnum = lnum + 1
      blocks = blocks + box(
        width: if start + sourcelines.len() > 999 { 30pt } else { 24pt },
        text(fill: rgb("#aaaaaa"), [ #lnum ]),
      )
    }
    blocks = blocks + ln + EndLine()
  }
  block(
    fill: code-bg,
    stroke: (left: 3pt + code-accent, rest: 0.5pt + code-border),
    inset: (x: 12pt, y: 10pt),
    radius: 4pt,
    width: 100%,
    above: 1em,
    below: 1em,
    breakable: true,
  )[
    #set par(leading: 0.55em, justify: false)
    #blocks
  ]
}
