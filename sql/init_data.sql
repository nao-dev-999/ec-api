COPY public.customer_order (id, customer_name, status, total_amount, created_at, updated_at, deleted_at, created_by, updated_by, deleted_by, version) FROM stdin;
2	鈴木一郎	PENDING	39100.00	2026-05-23 18:25:57.792385	2026-05-23 18:25:57.792432	\N	\N	\N	\N	0
1	山田太郎	CONFIRMED	97760.00	2026-05-17 16:39:30.488915	2026-05-25 01:47:35.981987	\N	\N	\N	\N	4
\.

--
-- Data for Name: customer_order_detail; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.customer_order_detail (id, customer_order_id, product_id, quantity, unit_price, subtotal, created_at, updated_at, deleted_at, created_by, updated_by, deleted_by, version) FROM stdin;
1	1	1	1	89800.00	89800.00	2026-05-17 16:39:30.53668	2026-05-17 16:39:30.536707	\N	\N	\N	\N	0
2	1	2	2	3980.00	7960.00	2026-05-17 16:39:30.564459	2026-05-17 16:39:30.564481	\N	\N	\N	\N	0
3	2	3	2	12800.00	25600.00	2026-05-23 18:25:57.925193	2026-05-23 18:25:57.92522	\N	\N	\N	\N	0
4	2	5	3	4500.00	13500.00	2026-05-23 18:25:57.95671	2026-05-23 18:25:57.956728	\N	\N	\N	\N	0
\.

--
-- Data for Name: product; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.product (id, name, description, price, stock, created_at, updated_at, deleted_at, created_by, updated_by, deleted_by, version) FROM stdin;
4	4Kモニター	27インチ IPS パネル HDR対応	49800.00	15	2026-05-17 16:19:47.95566	2026-05-17 16:19:47.95566	\N	\N	\N	\N	0
6	ヘッドセット	ノイズキャンセリング	15800.00	30	2026-05-17 16:39:18.776084	2026-05-17 16:39:18.776158	\N	\N	\N	\N	0
1	ノートPC	高性能薄型ノートPC 15インチ	89800.00	19	2026-05-17 16:19:47.95566	2026-05-17 16:39:30.592239	\N	\N	\N	\N	1
2	ワイヤレスマウス	静音設計 Bluetooth対応	3980.00	98	2026-05-17 16:19:47.95566	2026-05-17 16:39:30.597121	\N	\N	\N	\N	1
7	ヘッドセット	ノイズキャンセリング	15800.00	30	2026-05-23 16:49:55.181192	2026-05-23 16:49:55.181468	\N	\N	\N	\N	0
3	メカニカルキーボード	青軸 フルサイズ USB-C	12800.00	48	2026-05-17 16:19:47.95566	2026-05-23 18:25:57.979777	\N	\N	\N	\N	1
5	USBハブ	7ポート USB3.0 電源付き	4500.00	77	2026-05-17 16:19:47.95566	2026-05-23 18:25:57.986433	\N	\N	\N	\N	1
\.
