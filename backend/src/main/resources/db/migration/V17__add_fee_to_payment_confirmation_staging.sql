-- 決済突合バッチ（payment反映ロジック）でpayment.feeを算出するため、
-- 受信I/F（決済確定ファイル）にfee列が追加される前提でステージングにも同列を追加する。
-- 作業用テーブルのため監査カラムは持たない（V13/V15と同じ方針）。
ALTER TABLE payment_confirmation_staging
    ADD COLUMN fee DECIMAL(19, 2) NOT NULL DEFAULT 0;
