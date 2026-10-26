"""Create meter readings and billing cycles tables

Revision ID: e45d6c2e1dc3
Revises: 1686b036f6fc
Create Date: 2026-09-30 17:15:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'e45d6c2e1dc3'
down_revision: Union[str, Sequence[str], None] = '1686b036f6fc'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Create billing_cycles table
    op.create_table(
        'billing_cycles',
        sa.Column('id', sa.UUID(), nullable=False),
        sa.Column('user_id', sa.UUID(), nullable=False),
        sa.Column('cycle_id', sa.String(length=100), nullable=False),
        sa.Column('baseline_date', sa.Date(), nullable=False),
        sa.Column('baseline_reading', sa.Float(), nullable=False),
        sa.Column('previous_billed_units', sa.Float(), nullable=True),
        sa.Column('tariff_code', sa.String(length=100), nullable=True),
        sa.Column('service_connection_number', sa.String(length=50), nullable=True),
        sa.Column('is_active', sa.Boolean(), nullable=False, server_default=sa.text('true')),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_billing_cycles_user_id'), 'billing_cycles', ['user_id'], unique=False)
    op.create_index(op.f('ix_billing_cycles_cycle_id'), 'billing_cycles', ['cycle_id'], unique=False)

    # 2. Create meter_readings table
    op.create_table(
        'meter_readings',
        sa.Column('id', sa.UUID(), nullable=False),
        sa.Column('user_id', sa.UUID(), nullable=False),
        sa.Column('billing_cycle_id', sa.String(length=100), nullable=False),
        sa.Column('reading_value', sa.Float(), nullable=False),
        sa.Column('reading_date', sa.Date(), nullable=False),
        sa.Column('daily_consumption', sa.Float(), nullable=True),
        sa.Column('notes', sa.String(length=255), nullable=True),
        sa.Column('cycle_db_id', sa.UUID(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ),
        sa.ForeignKeyConstraint(['cycle_db_id'], ['billing_cycles.id'], ),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('user_id', 'billing_cycle_id', 'reading_date', name='uq_user_cycle_reading_date')
    )
    op.create_index(op.f('ix_meter_readings_user_id'), 'meter_readings', ['user_id'], unique=False)
    op.create_index(op.f('ix_meter_readings_billing_cycle_id'), 'meter_readings', ['billing_cycle_id'], unique=False)
    op.create_index(op.f('ix_meter_readings_reading_date'), 'meter_readings', ['reading_date'], unique=False)


def downgrade() -> None:
    op.drop_index(op.f('ix_meter_readings_reading_date'), table_name='meter_readings')
    op.drop_index(op.f('ix_meter_readings_billing_cycle_id'), table_name='meter_readings')
    op.drop_index(op.f('ix_meter_readings_user_id'), table_name='meter_readings')
    op.drop_table('meter_readings')

    op.drop_index(op.f('ix_billing_cycles_cycle_id'), table_name='billing_cycles')
    op.drop_index(op.f('ix_billing_cycles_user_id'), table_name='billing_cycles')
    op.drop_table('billing_cycles')
